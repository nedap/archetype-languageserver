'use strict';

import * as net from 'net';
import * as path from 'path';
import {Trace} from 'vscode-jsonrpc';
import { window, workspace, commands, ExtensionContext, Uri } from 'vscode';
import { LanguageClient, LanguageClientOptions} from 'vscode-languageclient/node';

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
    if(process.arch != 'x32' && process.arch != 'x64' && !(process.arch == 'arm64' && process.platform == 'darwin')) {
        throw 'unsupported CPU, this extension only runs on windows, macos or linux on x86 CPUs: ' + process.arch
    }
    if (process.platform == 'win32') {
        serverOptions = {
            run: {
                command: path.join(context.extensionPath , 'lsp-images', 'archie-lsp-winx64', 'bin', 'archie-lsp.bat')
            },
            debug: {
                command: path.join(context.extensionPath , 'lsp-images', 'archie-lsp-winx64', 'bin', 'archie-lsp.bat')
            }
        };
    }
    else if(process.platform == 'darwin' && process.arch == 'x64') {
        serverOptions = {
            run: {
                command: path.join(context.extensionPath , 'lsp-images', 'archie-lsp-macos-x64', 'bin', 'archie-lsp')
            },
            debug: {
                command: path.join(context.extensionPath , 'lsp-images', 'archie-lsp-macos-x64', 'bin', 'archie-lsp')
            }
        };
    } else if(process.platform == 'darwin' && process.arch == 'arm64') {
         serverOptions = {
             run: {
                 command: path.join(context.extensionPath , 'lsp-images', 'archie-lsp-macos-x64', 'bin', 'archie-lsp')
             },
             debug: {
                 command: path.join(context.extensionPath , 'lsp-images', 'archie-lsp-macos-x64', 'bin', 'archie-lsp')
             }
         };
     }
     else if (process.platform == 'linux') {
        serverOptions = {
            run: {
                command: path.join(context.extensionPath , 'lsp-images', 'archie-lsp-linux-x64', 'bin', 'archie-lsp')
            },
            debug: {
                command: path.join(context.extensionPath , 'lsp-images', 'archie-lsp-linux-x64', 'bin', 'archie-lsp')
            }
        };
    } else {
        throw 'unsupported platform, this extension only runs on windows, macos or linux on x86 CPUs, plus mac os on ARM CPUs: ' + process.platform
    }
    
    let clientOptions: LanguageClientOptions = {
        documentSelector: ['ADL', 'AQL'],
        synchronize: {
            fileEvents: workspace.createFileSystemWatcher('**/*.(adl|aql)*')
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