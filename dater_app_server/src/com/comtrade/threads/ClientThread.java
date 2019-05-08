package com.comtrade.threads;

import com.comtrade.controllerBL.ControllerBLogic;
import com.comtrade.domain.GeneralDomain;
import com.comtrade.domain.Message;
import com.comtrade.domain.Pictures;
import com.comtrade.domain.User;
import com.comtrade.transfer.TransferClass;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.comtrade.domain.Constants.*;
import static java.nio.file.Files.write;


public class ClientThread extends Thread implements Serializable {
    private Socket socket;
    private String currentUsername;
    private List<GeneralDomain> matches;


    public void setSocket(Socket socket) {
        this.socket = socket;
    }
    public void run() {

        while (true) {
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                TransferClass tf = (TransferClass) ois.readObject();
                processClientRequest(tf);
            } catch (IOException | ClassNotFoundException e) {
                ControllerBLogic.getInstance().removeActiveUser(currentUsername);
                e.printStackTrace();
            }
        }
    }

    private void processClientRequest(TransferClass tf) throws IOException {
        switch (tf.getOperation()){
            case SAVE_USER:
                User u = (User) tf.getClient_object();
                putUserInDataThread(u);
                break;

            case LOGIN_USER:
                Pictures p = new Pictures();
                User login = (User) tf.getClient_object();
                Map getAllUserList = ControllerBLogic.getInstance().getDatathread().getGetAllUserList();
                System.out.println("got login data");
                if(getAllUserList.containsKey(login.getUsername())){
                    User check = (User) getAllUserList.get(login.getUsername());
                    if(login.getPass().equals(check.getPass())){

                        Map testPicsforUser = testPicsUser();
                        try {
                            p = getPics(check);
                        }catch (Exception e){
                            System.out.println("nov user nema slike");
                        }

                        TransferClass ltf = new TransferClass();
                        check.setP(p);
                        ltf.setOperation(LOGIN);
                        ltf.setServer_object(check);
                        ControllerBLogic.getInstance().insertIntoActive(check.getUsername(), this);
                        sendToClient(ltf);

                    }
                }else if(!getAllUserList.containsKey(login.getUsername())){
                    TransferClass tce = new TransferClass();
                    tce.setOperation(WRONG_LOGIN);
                    sendToClient(tce);
                }
                break;
            case CHECK_USER:
                Map check = ControllerBLogic.getInstance().getDatathread().getGetAllUserList();
                User checkU = (User) tf.getClient_object();
                if(check.containsKey(checkU)){
                    TransferClass checktf = new TransferClass();
                    checktf.setOperation(USERNAME_TAKEN);
                    sendToClient(checktf);
                }
                break;

            case LIKE:
                User pics = (User) tf.getClient_object();
                savePics(pics);
                break;
        }
    }

    private Pictures getPics(User check) throws IOException {
        Pictures p = new Pictures();
        byte[] fileContent = new byte[0];
        List<File> list;
        File directory = new File("serverpics/"+check.getUsername());
        //get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if ( (!file.isDirectory()) && (file.getAbsolutePath().endsWith(".jpg") )) {
                System.out.println(file.getName());
                fileContent=Files.readAllBytes(file.toPath());
                p.getPictures().add(fileContent);
            }
        }

        return p;
    }

    private Map testPicsUser() {



        return null;
    }

    private Map getMatchesAndPics() {
        return null;
    }

    private void checkCredentials(User u) {
        Map getAllUserList = ControllerBLogic.getInstance().getDatathread().getGetAllUserList();
        Pictures p = new Pictures();
        TransferClass returnLogin = new TransferClass();
        if(getAllUserList.containsKey(u.getUsername())){
            User login = (User) getAllUserList.get(u.getUsername());
            if(login.getPass().equals(u.getPass())){
                TransferClass logged = new TransferClass();
                logged.setOperation(LOGIN_USER);
                logged.setServer_object(login);
                sendToClient(logged);

            }else{
                returnLogin.setOperation(WRONG_LOGIN);
                sendToClient(returnLogin);
            }
        }else if(!getAllUserList.containsKey(u.getUsername())){
            returnLogin.setOperation(WRONG_LOGIN);
            sendToClient(returnLogin);
        }


    }

    private void putUserInDataThread(User u) {
        ControllerBLogic.getInstance().getDatathread().getGetAllUserList().put(u.getUsername(), u);
        System.out.println("New User Added to Hash Map");
    }

    private void savePics(User u) throws IOException {
        List<byte[]> pictures = u.getP().getPictures();
        int length = pictures.size();
//        File theDir = new File(SERVERPICS);
        Path newDir = Paths.get(SERVERPICS + u.getUsername()+"/folder");
        Files.createDirectories(newDir);
        for(int i=0; i<length; i++){
            byte[] b = pictures.get(i);
            Path path = Paths.get(newDir + u.getUsername()+ i + ".jpg");
            try {
                write(path, b);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void loadPics(){

    }

    public void sendToClient(TransferClass tc2) {
        try {
            ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
            outStream.writeObject(tc2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sentMsgToOnlineFriend(Message pm) {
        TransferClass tc = new TransferClass();
        tc.setOperation(NEW_MESSAGE);
        tc.setServer_object(pm);
        sendToClient(tc);
    }





    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public List<GeneralDomain> getMatches() {
        return matches;
    }

    public void setMatches(List<GeneralDomain> matches) {
        this.matches = matches;
    }


}
