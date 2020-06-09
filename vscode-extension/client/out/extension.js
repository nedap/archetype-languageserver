'use strict';
Object.defineProperty(exports, "__esModule", { value: true });
exports.activate = void 0;
const net = require("net");
const vscode_jsonrpc_1 = require("vscode-jsonrpc");
const vscode_1 = require("vscode");
const vscode_languageclient_1 = require("vscode-languageclient");
function activate(context) {
    // The server is a started as a separate app and listens on port 1278
    let connectionInfo = {
        port: 1278
    };
    let serverOptions = () => {
        // Connect to language server via socket
        let socket = net.connect(connectionInfo);
        let result = {
            writer: socket,
            reader: socket
        };
        return Promise.resolve(result);
    };
    let clientOptions = {
        documentSelector: ['ADL'],
        synchronize: {
            fileEvents: vscode_1.workspace.createFileSystemWatcher('**/*.*')
        }
    };
    // Create the language client and start the client.
    let lc = new vscode_languageclient_1.LanguageClient('ADL Server', serverOptions, clientOptions);
    var disposable2 = vscode_1.commands.registerCommand("adl.a.proxy", async () => {
        let activeEditor = vscode_1.window.activeTextEditor;
        if (!activeEditor || !activeEditor.document || activeEditor.document.languageId !== 'plaintext') {
            return;
        }
        if (activeEditor.document.uri instanceof vscode_1.Uri) {
            vscode_1.commands.executeCommand("adl.a", activeEditor.document.uri.toString());
        }
    });
    context.subscriptions.push(disposable2);
    // enable tracing (.Off, .Messages, Verbose)
    lc.trace = vscode_jsonrpc_1.Trace.Verbose;
    let disposable = lc.start();
    // Push the disposable to the context's subscriptions so that the 
    // client can be deactivated on extension deactivation
    context.subscriptions.push(disposable);
}
exports.activate = activate;
//# sourceMappingURL=extension.js.map