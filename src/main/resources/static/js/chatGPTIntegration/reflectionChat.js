const entryIdField = document.getElementById('entryId');
if (entryIdField) {
    const entryId = entryIdField.value;
    const historyContainer = document.getElementById('reflection-history');
    const replyButton = document.getElementById('reply-button');
    const endButton = document.getElementById('end-reflection');
    const input = document.getElementById('reflection-input');
    let editingMessageId = null;

    async function sendReflection(text, messageId) {
        const payload = {message: text};
        if (messageId) {
            payload.messageId = String(messageId);
        }

        const res = await fetch(`/api/reflection/${entryId}/message`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if (!res.ok) {
            const err = await res.text();
            throw new Error(err);
        }
        return res.json();
    }

    async function fetchHistory() {
        const res = await fetch(`/api/reflection/${entryId}/history`);
        if (!res.ok) {
            throw new Error('Failed to load reflection history');
        }
        return res.json();
    }

    function clearHistory() {
        while (historyContainer.firstChild) {
            historyContainer.removeChild(historyContainer.firstChild);
        }
    }

    function renderHistory(messages) {
        clearHistory();
        messages.forEach(msg => {
            historyContainer.appendChild(buildMessageElement(msg));
        });
        historyContainer.scrollTop = historyContainer.scrollHeight;
    }

    function buildMessageElement(message) {
        const wrapper = document.createElement('div');
        wrapper.className = `reflection-msg ${message.role.toLowerCase()}`;

        const bubble = document.createElement('div');
        bubble.className = 'reflection-msg__bubble';
        const sender = message.role === 'USER' ? 'You' : 'AI';
        bubble.textContent = `${sender}: ${message.content}`;
        wrapper.appendChild(bubble);

        if (message.role === 'USER') {
            const actions = document.createElement('div');
            actions.className = 'reflection-msg__actions';
            const editButton = document.createElement('button');
            editButton.type = 'button';
            editButton.textContent = 'Edit';
            editButton.className = 'reflection-msg__edit';
            editButton.addEventListener('click', () => {
                editingMessageId = message.id;
                input.value = message.content;
                input.focus();
                replyButton.textContent = 'Update & Reply';
            });
            actions.appendChild(editButton);
            wrapper.appendChild(actions);
        }

        return wrapper;
    }

    async function refreshHistory() {
        try {
            const data = await fetchHistory();
            renderHistory(data.messages || []);
        } catch (error) {
            console.error(error);
        }
    }

    replyButton.addEventListener('click', async () => {
        const text = input.value.trim();
        if (!text) {
            return;
        }
        try {
            replyButton.disabled = true;
            const data = await sendReflection(text, editingMessageId);
            renderHistory(data.messages || []);
            input.value = '';
            editingMessageId = null;
            replyButton.textContent = 'Reply';
        } catch (e) {
            console.error(e);
            alert('Failed to send message');
        } finally {
            replyButton.disabled = false;
        }
    });

    input.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
            event.preventDefault();
            replyButton.click();
        }
    });

    endButton.addEventListener('click', () => {
        document.getElementById('reflection-section').style.display = 'none';
    });

    refreshHistory();
}