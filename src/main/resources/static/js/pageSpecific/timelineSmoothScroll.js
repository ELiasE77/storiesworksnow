document.addEventListener('DOMContentLoaded', function() {
    const timelineContainer = document.querySelector('.timeline');

    // Scroll timeline to the right
    document.getElementById('scroll-right').addEventListener('click', function() {
    timelineContainer.scrollBy({ left: 300, behavior: 'smooth' });
});

    // Scroll timeline to the left
    document.getElementById('scroll-left').addEventListener('click', function() {
        timelineContainer.scrollBy({ left: -300, behavior: 'smooth' });
    });
});
