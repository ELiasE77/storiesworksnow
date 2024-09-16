document.getElementById('journalForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const journalEntry = document.getElementById('journalEntry').value;

    const response = await fetch('http://localhost:8080/journal', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ entry: journalEntry }),
    });

    const result = await response.json();
    document.getElementById('response').innerHTML = `<p>${result.message}</p>`;
});
