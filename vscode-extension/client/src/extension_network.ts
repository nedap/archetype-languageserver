'use strict';

import * as net from 'net';

import {Trace} from 'vscode-jsonrpc';
import { window, workspace, commands, ExtensionContext, Uri } from 'vscode';
import { LanguageClient, LanguageClientOptions, StreamInfo} from 'vscode-languageclient/node';

export function activate(context: ExtensionContext) {
    // The server is a started as a separate app and listens on port 1278
    let connectionInfo = {
        port: 1278
    };
    let serverOptions = () => {
        // Connect to language server via socket
        let socket = net.connect(connectionInfo);
        let result: StreamInfo = {
            writer: socket,
            reader: socket
        };
        return Promise.resolve(result);
    };
    
    let clientOptions: LanguageClientOptions = {
        documentSelector: ['ADL', 'AQL'],
        synchronize: {
            fileEvents: workspace.createFileSystemWatcher('**/*.*')
        }
    };
    
    // Create the language client and start the client.
    let lc = new LanguageClient('ADL Server', serverOptions, clientOptions);

    // var disposable2 =commands.registerCommand("adl.a.proxy", async () => {
    //     let activeEditor = window.activeTextEditor;
    //     if (!activeEditor || !activeEditor.document || activeEditor.document.languageId !== 'plaintext') {
    //         return;
    //     }

    //     if (activeEditor.document.uri instanceof Uri) {
    //         commands.executeCommand("adl.a", activeEditor.document.uri.toString());
    //     }
    // })

   // context.subscriptions.push(disposable2);

    // enable tracing (.Off, .Messages, Verbose)
    lc.trace = Trace.Messages;
    let disposable = lc.start();
    
    // Push the disposable to the context's subscriptions so that the 
    // client can be deactivated on extension deactivation
    context.subscriptions.push(disposable);
}