document.addEventListener('DOMContentLoaded', () => {
    const contacts = Array.from(document.querySelectorAll('.contact-button'));
    const chatHistory = document.getElementById('chat-history');
    const chatHeader = document.getElementById('chat-header');
    const chatForm = document.getElementById('chat-form');
    const messageInput = document.getElementById('chat-message');
    const imageInput = document.getElementById('chat-image');
    const clearImageBtn = document.getElementById('clear-image');
    const selectedImageContainer = document.getElementById('selected-image');
    const currentUser = document.querySelector('.chat-main')?.getAttribute('data-current-user');

    let activeRecipient = null;
    let imageBase64 = '';

    if (contacts.length === 0) {
        chatHistory.innerHTML = '<p class="empty-state">Follow each other to start a conversation.</p>';
    }

    contacts.forEach(button => {
        button.addEventListener('click', async () => {
            contacts.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');
            activeRecipient = button.getAttribute('data-username');
            chatHeader.textContent = `Chatting with ${activeRecipient}`;
            await loadHistory(activeRecipient);
        });
    });

    if (contacts.length > 0) {
        contacts[0].click();
    }

    async function loadHistory(username) {
        chatHistory.innerHTML = '<p class="loading">Loading messages…</p>';
        try {
            const response = await fetch(`/chat/api/history/${username}`);
            if (!response.ok) {
                throw new Error('Failed to load chat history');
            }
            const messages = await response.json();
            renderMessages(messages);
        } catch (error) {
            console.error(error);
            chatHistory.innerHTML = '<p class="error">Unable to load messages.</p>';
        }
    }

    function renderMessages(messages) {
        chatHistory.innerHTML = '';
        messages.forEach(msg => {
            const row = document.createElement('div');
            row.classList.add('chat-message-row');
            if (msg.sender === currentUser) {
                row.classList.add('me');
            }

            const bubble = document.createElement('div');
            bubble.classList.add('chat-bubble');
            if (msg.content) {
                const text = document.createElement('p');
                text.textContent = msg.content;
                bubble.appendChild(text);
            }
            if (msg.imageData) {
                const img = document.createElement('img');
                img.src = `data:image/png;base64,${msg.imageData}`;
                bubble.appendChild(img);
            }

            const meta = document.createElement('div');
            meta.classList.add('chat-meta');
            meta.textContent = `${msg.sender} • ${new Date(msg.timestamp).toLocaleString()}`;

            row.appendChild(bubble);
            row.appendChild(meta);
            chatHistory.appendChild(row);
        });
        chatHistory.scrollTop = chatHistory.scrollHeight;
    }

    chatForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!activeRecipient) {
            alert('Select someone to chat with first.');
            return;
        }
        const content = messageInput.value.trim();
        if (!content && !imageBase64) {
            return;
        }
        try {
            const response = await fetch('/chat/api/send', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    recipient: activeRecipient,
                    content,
                    imageData: imageBase64
                })
            });
            if (!response.ok) {
                throw new Error('Failed to send message');
            }
            await response.json();
            messageInput.value = '';
            clearSelectedImage();
            // reload to include chronological order
            await loadHistory(activeRecipient);
        } catch (error) {
            console.error(error);
            alert('Unable to send message.');
        }
    });

    imageInput.addEventListener('change', async (event) => {
        const file = event.target.files[0];
        if (!file) {
            clearSelectedImage();
            return;
        }
        const reader = new FileReader();
        reader.onload = () => {
            const result = reader.result;
            const base64 = result.split(',')[1];
            imageBase64 = base64;
            selectedImageContainer.innerHTML = `<img src="${result}" alt="Selected image">`;
        };
        reader.readAsDataURL(file);
    });

    clearImageBtn.addEventListener('click', () => {
        clearSelectedImage();
    });

    function clearSelectedImage() {
        imageInput.value = '';
        imageBase64 = '';
        selectedImageContainer.innerHTML = '';
    }
});