async function sendReflection(entryId, text) {
    const res = await fetch(`/api/reflection/${entryId}/message`, {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({message: text})
    });
    if (!res.ok) {
        const err = await res.text();
        throw new Error(err);
    }
    const data = await res.json();
    return data.reply;
}

document.getElementById('reply-button').addEventListener('click', async () => {
    const i18n = window.reflectionI18n || {};
    const entryId = document.getElementById('entryId').value;
    const input = document.getElementById('reflection-input');
    const text = input.value.trim();
    if (!text) return;
    try {
        addMessage(i18n.userLabel || 'You', text);
        input.value = '';
        const reply = await sendReflection(entryId, text);
        addMessage(i18n.assistantLabel || 'AI', reply);
    } catch (e) {
        console.error(e);
        alert(e.message || i18n.sendError || 'Failed to send message');
    }
});

document.getElementById('end-reflection').addEventListener('click', () => {
    document.getElementById('reflection-section').style.display = 'none';
});

function addMessage(sender, text) {
    const container = document.getElementById('reflection-history');
    const div = document.createElement('div');
    div.className = 'reflection-msg';
    div.textContent = sender + ': ' + text;
    container.appendChild(div);
}
