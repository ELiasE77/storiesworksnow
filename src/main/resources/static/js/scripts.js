document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('journal-form');
    const contentInput = document.getElementById('journal-content');
    const entriesDiv = document.getElementById('journal-entries');



    // Fetch and display all journal entries on page load
    fetchEntries();

    // Form submission event handler
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const content = contentInput.value.trim();
        if (content) {
            await submitEntry(content);
            contentInput.value = '';
            fetchEntries(); // Refresh the entries after submission
        }
    });

    // Fetch journal entries from the server
    async function fetchEntries() {
        const response = await fetch('/api/journal');
        const entries = await response.json();
        displayEntries(entries);
    }

    // Display journal entries on the page
    function displayEntries(entries) {
        entriesDiv.innerHTML = '';
        entries.forEach(entry => {
            const entryDiv = document.createElement('div');
            entryDiv.classList.add('entry');
            entryDiv.innerHTML = `<p>${entry.content}</p><small>${entry.entryDate}</small>`;
            entriesDiv.appendChild(entryDiv);
        });
    }

    // Submit a new journal entry to the server
    async function submitEntry(content) {
        await fetch('/api/journal', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ content }),
        });
    }
});
