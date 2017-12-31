'use strict';

var statusElement = $('#status');
var nameInput = $('#name');
var usernamePage = $('#username-page');
var chatPage = $('#chat-page');
var messageInput = $('#message');
var messageArea = $('#messageArea');
var connectingElement = $('#connecting');

var stompClient = null;
var username = null;

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function connect(event) {
  username = nameInput.val().trim();
  if (username) {
    Cookies.set('name', username);
    usernamePage.hide();
    chatPage.show();

    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, onConnected, onError);
  }
  event.preventDefault();
}

function onConnected() {
  connectingElement.hide();
  stompClient.subscribe('/user/match', onMessageReceived);
  findMatch();
}

function findMatch() {
  stompClient.send('/app/addUser',
    {},
    JSON.stringify({sender: username, type: 'JOIN'})
  );
}

function findMatchOnClick(event) {
  findMatch();
  event.preventDefault();
}

function sendMessage(event) {
  var messageContent = messageInput.val().trim();
  if (messageContent && stompClient) {
    var chatMessage = {
      sender: username,
      content: messageContent,
      type: 'CHAT'
    };
    stompClient.send('/app/sendMessage', {}, JSON.stringify(chatMessage));
  }
  messageInput.val('');
  event.preventDefault();
}

function onMessageReceived(payload) {
  var message = JSON.parse(payload.body);

  switch(message.type) {
    case 'FINDING_MATCH':
      addEventMessage('Finding someone to talk to...');
      break;
    case 'FOUND_MATCH':
      addEventMessage('You are talking to ' + message.sender + '!');
      break;
    case 'LEAVE':
      addEventMessage(
        $('<span>')
          .append(message.sender + ' left! ')
          .append(
            $('<a>')
              .attr('onclick', 'findMatchOnClick()')
              .attr('href', '#')
              .text('Find another chat'))
          .append('?')
      );
      break;
    case 'CHAT':
      addChatMessage(message.sender, message.content);
      break;
  }
}

function addChatMessage(sender, content) {
  messageArea.append(
    $('<li>')
      .addClass('chat-message')
      .append($('<i>').css('background-color', getAvatarColor(sender)).text(sender[0]))
      .append($('<span>').text(sender))
      .append($('<p>').text(content))
  )
  messageArea.scrollTop(messageArea[0].scrollHeight);
}

function addEventMessage(content) {
  messageArea.append($('<li>').addClass('event-message').append($('<p>').append(content)));
  messageArea.scrollTop(messageArea[0].scrollHeight);
}

function onError(error) {
  connectingElement.text('Could not connect to WebSocket server. Please refresh this page to try again!');
  connectingElement.css('color', 'red');
}

function getAvatarColor(messageSender) {
  var hash = 0;
  for (var i = 0; i < messageSender.length; i++) {
      hash = 31 * hash + messageSender.charCodeAt(i);
  }
  var index = Math.abs(hash % colors.length);
  return colors[index];
}

$(document).ready(function() {
  var savedName = Cookies.get('name');
  if (savedName) {
    nameInput.val(savedName);
  }
  document.querySelector('#usernameForm').addEventListener('submit', connect, true);
  document.querySelector('#messageForm').addEventListener('submit', sendMessage, true);
});
