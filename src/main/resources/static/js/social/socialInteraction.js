function postJson(url, body) {
    return fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body || {})
    });
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.like-button').forEach(button => {
        button.addEventListener('click', async () => {
            const entryId = button.getAttribute('data-entry-id');
            try {
                const response = await postJson(`/api/social/${entryId}/likes/toggle`);
                if (!response.ok) {
                    throw new Error('Failed to toggle like');
                }
                const result = await response.json();
                button.classList.toggle('liked', result.liked);
                const countSpan = button.querySelector('.like-count');
                if (countSpan) {
                    countSpan.textContent = result.totalLikes;
                }
            } catch (error) {
                console.error(error);
                alert('Please log in to like posts.');
            }
        });
    });

    document.querySelectorAll('.comment-form').forEach(form => {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const textarea = form.querySelector('textarea');
            const content = textarea.value.trim();
            if (!content) {
                return;
            }
            const entryId = form.getAttribute('data-entry-id');
            try {
                const response = await postJson(`/api/social/${entryId}/comments`, { content });
                if (!response.ok) {
                    throw new Error('Failed to post comment');
                }
                const comment = await response.json();
                const list = form.parentElement.querySelector('.comment-list');
                const li = document.createElement('li');
                li.innerHTML = `<strong>${comment.author}</strong>: <span>${comment.content}</span> <small class="comment-timestamp">${comment.timestamp}</small>`;
                list.appendChild(li);
                textarea.value = '';
            } catch (error) {
                console.error(error);
                alert('Please log in to comment.');
            }
        });
    });
});