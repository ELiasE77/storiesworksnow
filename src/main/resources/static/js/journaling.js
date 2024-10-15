const speechToTextBtn = document.getElementById('speechToTextBtn');
const journalContent = document.getElementById('journal-content');
const speechIndicator = document.getElementById('speechIndicator');

// Check if the browser supports Speech Recognition
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
if (SpeechRecognition) {
    const recognition = new SpeechRecognition();

    recognition.continuous = false; // Stop recognition after one result
    recognition.interimResults = false; // We want final results only

    recognition.onstart = () => {
        speechIndicator.style.display = 'block'; // Show listening indicator
    };

    recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript; // Get the recognized text
        journalContent.value += (transcript + ' '); // Append it to the textarea
        speechIndicator.style.display = 'none'; // Hide the indicator
    };

    recognition.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        speechIndicator.style.display = 'none'; // Hide the indicator on error
    };

    recognition.onend = () => {
        speechIndicator.style.display = 'none'; // Hide the indicator when recognition ends
    };

    speechToTextBtn.addEventListener('click', () => {
        recognition.start();
    });
} else {
    console.warn('Speech Recognition is not supported in this browser.');
    speechToTextBtn.disabled = true; // Disable the button if not supported
}

// Function to call the Spring Boot API to get feedback
async function getFeedbackFromServer(text) {
    try {
        const response = await fetch('/api/get-feedback', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain'
            },
            body: text
        });

        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }

        const data = await response.json();
        return data.choices[0].message.content.trim();
    } catch (error) {
        console.error('Error fetching feedback:', error);
        throw error;
    }

}

// when clicking get feedback button it should use the get feedback function in ChatGPTController class and
// put that text in the journal entry box
document.getElementById('get-feedback').addEventListener('click', async function() {
    const journalText = document.getElementById('journal-content').value;
    if (journalText) {
        document.getElementById('feedback-text').innerText = 'Generating feedback...';

        try {
            const feedback = await getFeedbackFromServer(journalText);
            document.getElementById('feedback-text').innerText = feedback;
        } catch (error) {
            document.getElementById('feedback-text').innerText = 'Error generating feedback. Please try again.';
            console.error('Error fetching feedback:', error);
        }
    } else {
        alert('Please write something in the journal first.');
    }
});

async function generateImageFromJournalEntry(text) {
    const response = await fetch('/api/generate-image', {
        method: 'POST',
        headers: {
            'Content-Type': 'text/plain'
        },
        body: text
    });

    if (response.ok) {
        const data = await response.json();
        return data.imageUrl; //get the image url to display on the website
    } else {
        console.error('Error generating image:', await response.text());
        throw new Error('Image generation failed');
    }
}

// Button click handler for generate image button
document.getElementById('generate-image').addEventListener('click', async function() {
    const journalText = document.getElementById('journal-content').value;
    if (journalText) {
        try {
            const imageUrl = await generateImageFromJournalEntry(journalText);
            const imageElement = document.getElementById('generated-image');
            imageElement.src = imageUrl;
            imageElement.style.display = 'block'; // Show the image
        } catch (error) {
            alert('Failed to generate image. Please try again.');
        }
    } else {
        alert('Please write something in the journal first.');
    }
});

