// Function to call the Spring Boot API to get feedback
async function getFeedbackFromServer(text) {
    try {
        const response = await fetch('/api/get-feedback', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ content: text })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }

        const data = await response.json();
        // Our controller now always returns { "feedback": "..." }
        return data.feedback ? data.feedback.trim() : "No feedback received.";
    } catch (error) {
        console.error('Error fetching feedback:', error);
        throw error;
    }
}

// When clicking get feedback button, fetch feedback and show in UI
document.getElementById('get-feedback').addEventListener('click', async function() {
    const journalText = document.getElementById('journal-content').value;
    const feedbackButton = document.getElementById('get-feedback');

    if (journalText) {
        document.getElementById('feedback-text').innerText = 'Generating feedback...';
        feedbackButton.disabled = true;

        try {
            const feedback = await getFeedbackFromServer(journalText);
            document.getElementById('feedback-text').innerText = feedback;
        } catch (error) {
            document.getElementById('feedback-text').innerText =
                'Error generating feedback. Please try again.';
        } finally {
            feedbackButton.disabled = false;
        }
    } else {
        alert('Please write something in the journal first.');
    }
});
