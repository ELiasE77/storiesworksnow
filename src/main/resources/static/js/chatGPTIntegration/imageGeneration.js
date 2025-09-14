// Wait until DOM is fully loaded
document.addEventListener('DOMContentLoaded', () => {
    const styleOptions = document.querySelectorAll('.style-option');

    // Handle style selection
    styleOptions.forEach(option => {
        option.addEventListener('click', function () {
            styleOptions.forEach(opt => opt.classList.remove('selected'));
            this.classList.add('selected');
            document.getElementById('selected-style').value = this.getAttribute('data-style');
        });
    });

    const generateButton = document.getElementById('generate-image');
    const loadingIndicator = document.getElementById('loading-indicator');
    const hiddenInput = document.getElementById('imageUrl');
    const imageElement = document.getElementById('generated-image');

    if (!generateButton) {
        console.error('Generate Image button not found');
        return;
    }

    generateButton.addEventListener('click', async function () {
        const journalText = document.getElementById('journal-content').value;
        const selectedStyle = document.getElementById('selected-style').value;

        if (!journalText) {
            alert('Please write something in the journal first.');
            return;
        }

        loadingIndicator.style.display = 'block';
        generateButton.disabled = true;

        try {
            // Call backend to generate new image
            const base64Image = await generateImageFromJournalEntry(journalText, selectedStyle);

            // Show preview with data prefix
            imageElement.src = `data:image/png;base64,${base64Image}`;
            imageElement.style.display = 'block';

            // Update hidden input with RAW base64 (no prefix!)
            hiddenInput.value = base64Image;

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
async function generateImageFromJournalEntry(text, style) {
    const response = await fetch('/api/generate-image', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ journalText: text, style: style })
    });

    if (response.ok) {
        const jsonResponse = await response.json();
        return jsonResponse.base64Image; // raw base64 string
    } else {
        console.error('Error generating image:', await response.text());
        throw new Error('Image generation failed');
    }
}
