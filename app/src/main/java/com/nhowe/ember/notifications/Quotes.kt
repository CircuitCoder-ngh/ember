package com.nhowe.ember.notifications

/** Short, well-known lines to pair with the hourly progress card. */
object Quotes {
    private val ALL = listOf(
        "We are what we repeatedly do. Excellence, then, is not an act but a habit." to "Will Durant",
        "The secret of getting ahead is getting started." to "Mark Twain",
        "It always seems impossible until it's done." to "Nelson Mandela",
        "Well begun is half done." to "Aristotle",
        "A journey of a thousand miles begins with a single step." to "Lao Tzu",
        "Do what you can, with what you have, where you are." to "Theodore Roosevelt",
        "Small deeds done are better than great deeds planned." to "Peter Marshall",
        "What you do today can improve all your tomorrows." to "Ralph Marston",
        "Discipline is choosing between what you want now and what you want most." to "Abraham Lincoln",
        "The best time to plant a tree was 20 years ago. The second best time is now." to "Chinese proverb",
        "Action is the foundational key to all success." to "Pablo Picasso",
        "You don't have to be great to start, but you have to start to be great." to "Zig Ziglar",
        "Either you run the day or the day runs you." to "Jim Rohn",
        "Motivation gets you going, but discipline keeps you growing." to "John C. Maxwell",
        "Success is the sum of small efforts, repeated day in and day out." to "Robert Collier",
        "Don't watch the clock; do what it does. Keep going." to "Sam Levenson",
        "The way to get started is to quit talking and begin doing." to "Walt Disney",
        "It does not matter how slowly you go as long as you do not stop." to "Confucius",
        "Perseverance is not a long race; it is many short races one after the other." to "Walter Elliot",
        "You miss 100% of the shots you don't take." to "Wayne Gretzky",
        "Every accomplishment starts with the decision to try." to "John F. Kennedy",
        "Energy and persistence conquer all things." to "Benjamin Franklin",
        "Start where you are. Use what you have. Do what you can." to "Arthur Ashe",
        "Nothing will work unless you do." to "Maya Angelou",
        "Fall seven times, stand up eight." to "Japanese proverb",
        "The future depends on what you do today." to "Mahatma Gandhi",
        "Quality is not an act, it is a habit." to "Aristotle",
        "Believe you can and you're halfway there." to "Theodore Roosevelt",
        "Little by little, one travels far." to "J.R.R. Tolkien",
        "First, have a definite, clear practical ideal. Second, have the means. Third, adjust all your means to that end." to "Aristotle",
        "Your future is created by what you do today, not tomorrow." to "Robert Kiyosaki",
        "Great things are done by a series of small things brought together." to "Vincent van Gogh",
        "Dreams don't work unless you do." to "John C. Maxwell",
        "One day or day one. You decide." to "Unknown",
        "Done is better than perfect." to "Sheryl Sandberg",
        "If you get tired, learn to rest, not to quit." to "Banksy",
        "Keep the flame lit." to "Ember",
    )

    fun random(seed: Long = System.currentTimeMillis()): String {
        val (text, author) = ALL[(seed / (60L * 60L * 1000L)).mod(ALL.size)]
        return "“$text” — $author"
    }
}
