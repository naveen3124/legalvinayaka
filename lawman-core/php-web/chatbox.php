
<?php
	/*
		Template Name: Custom Chatbox Page Template
	*/
	
	get_header(); // Include the header of your theme
?>

<!-- HTML structure for the chatbox -->
<div id="chatbox">
  <div id="chat-messages"></div>
  <input type="text" id="message-input" placeholder="Type your message...">
  <button id="send-button">Send</button>
</div>

<?php
	get_footer(); // Include the footer of your theme
?>

