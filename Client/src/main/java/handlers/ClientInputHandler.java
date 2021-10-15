package handlers;

import command.Command;
import command.ServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;


public class ClientInputHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object object) throws FileNotFoundException {
        if (object instanceof String) {
            processResponse((String) object, channelHandlerContext);
            return;
        }
        if (object instanceof File[]) {
            processFileList((File[]) object, channelHandlerContext);
            return;
        }
        if (object instanceof File) {
            processFile((File) object, channelHandlerContext);
        }
    }


    private void processResponse(String message, ChannelHandlerContext channelHandlerContext) throws FileNotFoundException {
        String[] parameters = message.split(" ");
        String response = parameters[0];

        // TODO: add logging instead of output to stdout.
        if (ServerResponse.AUTH_OK.equals(message)) {
            System.out.println("client.Client authentication successful.");
        } else if (ServerResponse.AUTH_FAIL.equals(message)) {
            System.out.println("client.Client authentication failed.");
        } else if (ServerResponse.COPY_OK.equals(response)) {
            String src = parameters[1];
            String dest = parameters[2];
            System.out.printf("File copy %s -> %s successful.\n", src, dest);
        } else if (ServerResponse.COPY_FAIL.equals(response)) {
            String src = parameters[1];
            String dest = parameters[2];
            System.out.printf("File copy %s -> %s failed.\n", src, dest);
        } else if (ServerResponse.MKDIR_OK.equals(response)) {
            String dir = parameters[1];
            System.out.printf("Create directory %s successful.\n", dir);
        } else if (ServerResponse.MKDIR_FAIL.equals(response)) {
            String dir = parameters[1];
            System.out.printf("Create directory %s failed.\n", dir);
        } else if (ServerResponse.DELETE_OK.equals(response)) {
            String file = parameters[1];
            System.out.printf("Delete file %s successful.\n", file);
        } else if (ServerResponse.DELETE_FAIL.equals(response)) {
            String file = parameters[1];
            System.out.printf("Delete file %s failed.\n", file);
        } else if (ServerResponse.UPLOAD_READY.equals(response)) {
            String filePath = parameters[1];
            receiveFile(filePath, channelHandlerContext);
        } else if (ServerResponse.DOWNLOAD_READY.equals(response)) {
            String filePath = parameters[2];
            sendFile(filePath, channelHandlerContext);
        } else {
            System.out.println(message);
        }
    }

    /**
     * Sends specified file to the client.
     *
     * @param filePath              Path to the file for sending to the client.
     * @param channelHandlerContext Channel handler context.
     */
    private void sendFile(String filePath, ChannelHandlerContext channelHandlerContext) throws FileNotFoundException {
        System.out.println("Server is ready to send the file.");
        byte[] buffer = new byte[10];
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw")){
            while (true) {
                int read = randomAccessFile.read(buffer);
                long position = randomAccessFile.getFilePointer();
                randomAccessFile.seek(1024);
                if (read < buffer.length - 1) {
                    byte[] tempBuffer = new byte[read];
                    System.arraycopy(buffer, 0, tempBuffer, 0, read);
                    channelHandlerContext.writeAndFlush(tempBuffer);
                    channelHandlerContext.flush();
                    break;
                }
                channelHandlerContext.writeAndFlush(buffer);
            }
            buffer = new byte[1024*512];
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

        /**
         * Receives data from the client and writes it to the specified file.
         *
         * @param filePath              Path to the file for writing received data from the client.
         * @param channelHandlerContext Channel handler context.
         */
        private void receiveFile (String filePath, ChannelHandlerContext channelHandlerContext){
            System.out.println("Server is ready to receive the file.");
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.printf("File %s does not exist.\n", filePath);
                return;
            }
            byte[] buffer = new byte[1024*512];
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw")){
                while (true) {
                    int read = randomAccessFile.read(buffer);
                    long position = randomAccessFile.getFilePointer();
                    randomAccessFile.seek(1024);
                    if (read < buffer.length - 1) {
                        byte[] tempBuffer = new byte[read];
                        System.arraycopy(buffer, 0, tempBuffer, 0, read);
                        channelHandlerContext.writeAndFlush(tempBuffer);
                        channelHandlerContext.flush();
                        break;
                    }
                    channelHandlerContext.writeAndFlush(buffer);
                }
                buffer = new byte[1024*512];
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void processFileList (File[]fileList, ChannelHandlerContext channelHandlerContext){
            System.out.println("Current server directory file list:");
            for (File file : fileList) {
                System.out.println(file.getPath());
            }
            channelHandlerContext.writeAndFlush(fileList);
        }

        private void processFile (File file, ChannelHandlerContext channelHandlerContext){
            System.out.println("Current server directory:");
            System.out.println(file.getPath());
            channelHandlerContext.writeAndFlush(file);
        }
    }
