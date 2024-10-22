async function generateImageFromJournalEntry(text) {
    const response = await fetch('/api/generate-image', {
        method: 'POST',
        headers: {
            'Content-Type': 'text/plain'
        },
        body: text
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
    const generateButton = document.getElementById('generate-image');
    const loadingIndicator = document.getElementById('loading-indicator');


    if (journalText) {
        // Show loading indicator and disable button
        loadingIndicator.style.display = 'block';
        generateButton.disabled = true;

        try {
            const base64Image = await generateImageFromJournalEntry(journalText);

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