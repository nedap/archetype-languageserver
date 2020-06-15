'use strict';

import * as net from 'net';

import {Trace} from 'vscode-jsonrpc';
import { window, workspace, commands, ExtensionContext, Uri } from 'vscode';
import { LanguageClient, LanguageClientOptions, ExecutableOptions, Executable, StreamInfo, Position as LSPosition, Location as LSLocation, TransportKind } from 'vscode-languageclient';

export function activate(context: ExtensionContext) {
    // The server is a started as a separate app and listens on port 1278
    let connectionInfo = {
        port: 1278
    };
    // let serverOptions = () => {
    //     // Connect to language server via socket
    //     let socket = net.connect(connectionInfo);
    //     let result: StreamInfo = {
    //         writer: socket,
    //         reader: socket
    //     };
    //     return Promise.resolve(result);
    // };

    let serverOptions;
    if(process.platform == 'win32') {
        serverOptions = { 
            run: {  
                command: context.extensionPath + '\\archie-lsp-shadow\\bin\\archie-lsp.bat'
            },
            debug: {  
                command: context.extensionPath + '\\archie-lsp-shadow\\bin\\archie-lsp.bat'
            }
        }
    } else {
        serverOptions = {
            run: {  
                command: context.extensionPath + '/archie-lsp-shadow/bin/archie-lsp'
            },
            debug: {  
                command: context.extensionPath + '/archie-lsp-shadow/bin/archie-lsp'
            }
        }
    }
    
    let clientOptions: LanguageClientOptions = {
        documentSelector: ['ADL'],
        synchronize: {
            fileEvents: workspace.createFileSystemWatcher('**/*.adl*')
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
    lc.trace = Trace.Verbose;
    
    let disposable = lc.start();
    
    // Push the disposable to the context's subscriptions so that the 
    // client can be deactivated on extension deactivation
    context.subscriptions.push(disposable);
}