// Wait until DOM is fully loaded
document.addEventListener('DOMContentLoaded', () => {
    const styleOptions = document.querySelectorAll('.style-option');
    const generateButton = document.getElementById('generate-image');
    const loadingIndicator = document.getElementById('loading-indicator');
    const imageUrlInput = document.getElementById('imageUrl');
    const generatedImage = document.getElementById('generated-image');
    const generatedImageContainer = document.getElementById('generated-image-container');
    const pictureTypeSelect = document.getElementById('pictureType');
    const fileInput = document.getElementById('image-upload');

    // Handle style selection
    styleOptions.forEach(option => {
        option.addEventListener('click', function () {
            styleOptions.forEach(opt => opt.classList.remove('selected'));
            this.classList.add('selected');
            document.getElementById('selected-style').value = this.getAttribute('data-style');
        });
    });

    // Handle picture type (regular / personalized / none)
    if (pictureTypeSelect) {
        pictureTypeSelect.addEventListener('change', function () {
            if (this.value === "none") {
                // Clear and hide any image
                const oldImg = document.getElementById('current-image');
                if (oldImg) oldImg.remove();

                generatedImage.src = "";
                generatedImage.style.display = "none";
                generatedImageContainer.style.display = "none";

                // Reset hidden input
                imageUrlInput.value = "";
            }
        });
    }

    if (!generateButton) {
        console.error('Generate Image button not found');
        return;
    }

    if (fileInput) {
        fileInput.addEventListener('change', (event) => {
            const target = event.target;
            const file = target.files && target.files[0];
            if (!file) {
                return;
            }

            const reader = new FileReader();
            reader.onload = e => {
                const result = e.target?.result;
                if (typeof result !== 'string') {
                    return;
                }
                const commaIndex = result.indexOf(',');
                const base64Image = commaIndex >= 0 ? result.substring(commaIndex + 1) : result;

                const oldImg = document.getElementById('current-image');
                if (oldImg) {
                    oldImg.remove();
                }

                imageUrlInput.value = base64Image;
                generatedImage.src = `data:image/png;base64,${base64Image}`;
                generatedImage.style.display = 'block';
                generatedImageContainer.style.display = 'block';
            };
            reader.readAsDataURL(file);
        });
    }

    generateButton.addEventListener('click', async function () {
        const journalText = document.getElementById('journal-content').value;
        const selectedStyle = document.getElementById('selected-style').value;
        const pictureType = pictureTypeSelect?.value || "regular";
        const persona = document.getElementById('persona-feature')?.value || "";

        if (!journalText) {
            alert('Please write something in the journal first.');
            return;
        }

        if (pictureType === "none") {
            alert("You selected 'No picture'. Change the option to generate an image.");
            return;
        }

        // Show loading indicator and disable button
        loadingIndicator.style.display = 'block';
        generateButton.disabled = true;

        try {
            const base64Image = await generateImageFromJournalEntry(journalText, selectedStyle, pictureType, persona);

            // Replace old current-image preview if exists
            const oldImg = document.getElementById('current-image');
            if (oldImg) {
                oldImg.remove();
            }

            // Show the new generated image
            generatedImage.src = `data:image/png;base64,${base64Image}`;
            generatedImage.style.display = 'block';
            generatedImageContainer.style.display = 'block';

            // Store raw base64 in hidden input for later form submission
            imageUrlInput.value = base64Image;

        } catch (error) {
            console.error(error);
            alert('Failed to generate image. Please try again.');
        } finally {
            loadingIndicator.style.display = 'none';
            generateButton.disabled = false;
        }
    });
});

// Function to call backend API
async function generateImageFromJournalEntry(text, style, pictureType, persona) {
    const response = await fetch('/api/generate-image', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            journalText: text,
            style: style,
            pictureType: pictureType,
            persona: persona
        })
    });

    if (response.ok) {
        const jsonResponse = await response.json();
        return jsonResponse.base64Image; // Extract raw Base64 image string
    } else {
        console.error('Error generating image:', await response.text());
        throw new Error('Image generation failed');
    }
}
