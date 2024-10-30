document.getElementById('follow-btn').addEventListener('click', function() {

    const username = this.getAttribute('data-username');
    const isFollowing = this.textContent.trim() === 'Unfollow';
    const actionUrl = isFollowing ? `/user/unfollow/${username}` : `/user/follow/${username}`;

    fetch(actionUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
        .then(response => response.text())
        .then(data => {
            // Toggle the button text and class based on the response
            if (isFollowing) {
                this.textContent = 'Follow';
            } else {
                this.textContent = 'Unfollow';
            }
            alert(data);  // Display message returned from the server
        })
        .catch(error => console.error('Error:', error));
});