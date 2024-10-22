# DigitalLife Journaling
Created in service of Digital Life to serve as a platform to share personal
stories, create image galleries based on those stories, improve your journal
writing skills and connect with others through those experiences.

##The Team
- Xander Heinen (Developer)
- Elias (AI model trainer)
- Janneke (website design)
- Don (AI model trainer)
- George (business model)


##Implementation
###Current code stack
- Java (back-end)
- html/css (front-end layout)
- javascript (page interactivity)
- MySql (database)

##current functionality
###Pages
- A page to create journal entries (journal.html)
- A page to edit created journal entries (journal_editing.html)
- A page to create a community (communityCreator.html)
- A page with an overview of all communities of user (communityOverview.html)
- A page with an overview of all journal entries (social_home.html)
- A home page (home.html)

###Features currently working
- generate image based on journal entry
- generate feedback on the writing of a journal entry
- 

###Security
The following work regarding security has already been implemented:
- password hashing using Bcrypt (see securityConfig class)
- basic session management (could improve on coockie handling)
- API Key rate limiting and safe storage of API Key (you cannot spam the generate
feedback or image button)
- Protection against SQL injection (only prepared statements are used using Spring Boot)

##Future work
###Security
There are several security concerns in the current codebase which 
are not yet addressed, I listed all I found below, be warned that 
other issues could be present which are not listed below:
- validating API responses (make sure no malicious data is received from API)
- XSS (Cross-Site scripting) protection: All user generated content like communities, 
journalEntries and other editable fields need to be sanitized. Check for data types
and malicious scripts.
- CSRF protection: Make sure that the one sending a form is actually the user
- Role Based Access Control: Make sure that users can only edit their own information.
 Currently, a user could technically edit another journal entry if they know the url. 
Make sure that only the current user can access it.
- HTTPS instead of HTTP

###Possible additions
- to be discussed