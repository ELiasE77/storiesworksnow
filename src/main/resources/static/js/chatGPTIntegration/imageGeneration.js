// Select all style option images
const styleOptions = document.querySelectorAll('.style-option');


// Handle style selection
styleOptions.forEach(option => {
    option.addEventListener('click', function() {
        // Remove 'selected' class from all options
        styleOptions.forEach(opt => opt.classList.remove('selected'));

        // Add 'selected' class to the clicked option
        this.classList.add('selected');

        // Set the selected style value in the hidden input
        document.getElementById('selected-style').value = this.getAttribute('data-style');
    });
});

async function generateImageFromJournalEntry(text, style) {
    const response = await fetch('/api/generate-image', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            journalText: text,
            style: style
        })
    });

    if (response.ok) {
        const jsonResponse = await response.json(); // Parse the JSON response
        return jsonResponse.base64Image; // Extract the Base64 image string
    } else {
        console.error('Error generating image:', await response.text());
        throw new Error('Image generation failed');
    }
}

// Button click handler for generate image button
document.getElementById('generate-image').addEventListener('click', async function() {
    const journalText = document.getElementById('journal-content').value;
    const selectedStyle = document.getElementById('selected-style').value;
    const generateButton = document.getElementById('generate-image');
    const loadingIndicator = document.getElementById('loading-indicator');


    if (journalText) {
        // Show loading indicator and disable button
        loadingIndicator.style.display = 'block';
        generateButton.disabled = true;

        try {
            const base64Image = await generateImageFromJournalEntry(journalText, selectedStyle);

            const imageElement = document.getElementById('generated-image');
            imageElement.src = `data:image/png;base64,${base64Image}`;


            imageElement.style.display = 'block'; // Show the image
            document.getElementById('imageUrl').value = base64Image;
        } catch (error) {
            alert('Failed to generate image. Please try again.');
        } finally {
            // Hide loading indicator and re-enable button
            loadingIndicator.style.display = 'none';
            generateButton.disabled = false;
        }
    } else {
        alert('Please write something in the journal first.');
    }
});