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