jQuery(document).ready(function($) {
  // Define the AJAX URL to your PHP script
  var ajaxUrl = ajax_object.ajax_url;

  function displayMessage(message, isSent = false) {
    // Create a new chat bubble element
    var chatBubble = $('<div class="chat-bubble"></div>');

    // Set the content of the chat bubble to the message
    chatBubble.text(message);

    // Add the 'sent' class to the chat bubble if it's a sent message
    if (isSent) {
      chatBubble.addClass('sent');
    }

    // Append the chat bubble to the chat messages container
    $('#chat-messages').append(chatBubble);
  }

  // Function to send a message via AJAX
  function sendMessage(message) {
    $.ajax({
      url: ajaxUrl,
      method: 'POST',
      data: {
        action: 'send_message',
        message: message
      },
      success: function(response) {
        // Display the received message in the chatbox
        displayMessage(response, false); // 'isSent' is set to false for received messages
      }
    });
  }

  // Send button click event handler
  $('#send-button').on('click', function() {
    var message = $('#message-input').val();
    if (message.trim() !== '') {
      // Display the sent message in the chatbox as a sent message
      displayMessage(message, true);

      // Send the message via AJAX
      sendMessage(message);
      $('#message-input').val('');
    }
  });
  // Handle pressing Enter key to send the message
  $('#message-input').keypress(function(event) {
    if (event.which === 13) {
      event.preventDefault();
      var message = $(this).val();
      if (message.trim() !== '') {
        sendMessage(message);
        $(this).val('');
      }
    }
  });
});