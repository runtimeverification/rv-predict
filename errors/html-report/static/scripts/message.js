/**
 * This script file is used for RV-Toolkit website report page iframe communication.
 */
window.addEventListener("message", receiveMessage, false);

/**
 * Receive message and execute script.
 * @param {MessageEvent} event 
 */
function receiveMessage(event) {
  if (event.data && typeof(event.data) === 'object' && event.data.type === 'rv-toolkit-web-request') {
    const script = event.data.params;
    try {
      const result = eval(script);
      window.parent.postMessage({
        "type": "rv-toolkit-web-response",
        "result": result 
      }, "*");
    } catch(error) {
      window.parent.postMessage({
        "type": "rv-toolkit-web-response",
        "error": error 
      }, "*");
    }
  }
}

