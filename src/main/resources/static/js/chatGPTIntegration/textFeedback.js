async function getFeedbackFromServer(text) {
    const response = await fetch('/api/get-feedback', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ content: text })
    });

    if (!response.ok) {
        throw new Error(await readApiError(response));
    }

    const data = await response.json();
    return {
        feedback: data.feedback ? data.feedback.trim() : (window.journallyI18n?.feedbackEmpty || 'No feedback received.'),
        questions: Array.isArray(data.questions) ? data.questions : [],
        sentimentScore: Number.isFinite(Number(data.sentimentScore)) ? Number(data.sentimentScore) : null,
        visualMemoryQuestionIndex: Number.isInteger(Number(data.visualMemoryQuestionIndex))
                ? Number(data.visualMemoryQuestionIndex)
                : 2
    };
}

async function readApiError(response) {
    const raw = await response.text();
    try {
        const json = JSON.parse(raw);
        return json.error || raw || `HTTP error! Status: ${response.status}`;
    } catch (error) {
        return raw || `HTTP error! Status: ${response.status}`;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const i18n = window.journallyI18n || {};
    const msg = (key, fallback) => i18n[key] || fallback;
    const feedbackButton = document.getElementById('get-feedback');

    async function generateJournalFeedback(options = {}) {
        const journalText = document.getElementById('journal-content')?.value;
        const feedbackText = document.getElementById('feedback-text');

        if (!journalText) {
            if (!options.quiet) {
                alert(msg('writeFirstAlert', 'Please write something in the journal first.'));
            }
            return;
        }

        if (feedbackText) {
            feedbackText.textContent = msg('feedbackLoading', 'Generating feedback...');
        }
        if (feedbackButton) {
            feedbackButton.disabled = true;
        }

        try {
            const { feedback, questions, sentimentScore, visualMemoryQuestionIndex } = await getFeedbackFromServer(journalText);
            if (feedbackText) {
                feedbackText.textContent = feedback;
            }
            if (Number.isFinite(sentimentScore)) {
                window.currentAiSentimentScore = sentimentScore;
            }

            if (typeof window.renderReflectionPrompts === 'function') {
                const existingPrompts = typeof window.collectEntryData === 'function'
                        ? window.collectEntryData().reflectionPrompts || []
                        : [];

                const answersByQuestion = new Map(
                        existingPrompts.map((prompt) => [prompt.question, prompt.answer || ''])
                );

                window.renderReflectionPrompts(
                        questions.map((question, index) => ({
                            question,
                            answer: answersByQuestion.get(question) || '',
                            kind: index === visualMemoryQuestionIndex ? 'VISUAL_MEMORY' : 'REFLECTION'
                        }))
                );
            }
        } catch (error) {
            if (feedbackText) {
                feedbackText.textContent = error.message || msg('feedbackError', 'Error generating feedback. Please try again.');
            }
        } finally {
            if (feedbackButton) {
                feedbackButton.disabled = false;
            }
        }
    }

    window.generateJournalFeedback = generateJournalFeedback;
    feedbackButton?.addEventListener('click', () => generateJournalFeedback());
});
