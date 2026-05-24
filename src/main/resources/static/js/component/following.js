document.getElementById('follow-btn').addEventListener('click', function() {
    const i18n = window.followI18n || {};

    const username = this.getAttribute('data-username');
    const isFollowing = this.getAttribute('data-following') === 'true';
    const actionUrl = isFollowing ? `/user/unfollow/${username}` : `/user/follow/${username}`;

    fetch(actionUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
        .then(response => response.text())
        .then(() => {
            // Toggle the button text and class based on the response
            if (isFollowing) {
                this.textContent = i18n.follow || 'Follow';
                this.setAttribute('data-following', 'false');
            } else {
                this.textContent = i18n.unfollow || 'Unfollow';
                this.setAttribute('data-following', 'true');
            }
            alert(isFollowing ? (i18n.unfollowSuccess || 'Unfollowed user.') : (i18n.followSuccess || 'Followed user.'));
        })
        .catch(error => {
            console.error('Error:', error);
            alert(i18n.toggleError || 'Could not update follow state.');
        });
});
