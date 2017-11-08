document.getElementById("save").onclick = function(event) {
  chrome.storage.local.clear();

  var startPage = parseInt(document.getElementById("pagestart").value);
  var pageCount = parseInt(document.getElementById("pagecount").value);

  var message = {action: "save", start: startPage, count: pageCount}

  chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
    chrome.tabs.sendMessage(tabs[0].id, message, function(response) {
    });
  });
};
