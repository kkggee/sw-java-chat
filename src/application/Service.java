package application;
import java.io.BufferedReader;

import java.io.IOException;

import java.io.InputStreamReader;

import java.io.OutputStream;

import java.net.Socket;

import java.util.Vector;

public class Service extends Thread{
   Room myRoom;//클라이언트가 입장한 대화방
   
   //소켓관련 입출력서비스
   BufferedReader in;
   OutputStream out;

   Vector<Service> allV;  //모든 사용자(대기실사용자 + 대화방사용자)
   Vector<Service> waitV; //대기실 사용자      
   Vector<Room> roomV;   //개설된 대화방(대화방사용자)
   Socket s;
   String nickName, nickName1;
   String newname;
   int num=0;
   
   public Service(Socket s, Server server) { 
      allV=server.allV;
      waitV=server.waitV;
      roomV=server.roomV; 
      
      this.s = s;
      
      try {      
      in = new BufferedReader(new InputStreamReader(s.getInputStream()));
      out = s.getOutputStream();
      
      start();
     } catch (IOException e) {
      e.printStackTrace();
    }
   } //생성자 끝
   
   @Override
   public void run() {
    try {
      while(true){
         String msg = in.readLine();//클라이언트의 모든 메시지를 받기
         if(msg == null) return; //비정상적인 종료
         if(msg.trim().length() > 0){  
           System.out.println("from Client: "+ msg +":"+s.getInetAddress().getHostAddress());

            String msgs[]=msg.split("\\|");
            String protocol = msgs[0]; 
            
          switch(protocol){
            case "100": //대기실 접속
                  allV.add(this); //전체사용자에 등록
                  waitV.add(this); //대기실사용자에 등록 
                  break;

            case "150": //대화명 입력
               nickName=msgs[1]; 
               nickName1 = msgs[1];
               //최초 대화명 입력했을때 대기실의 정보를 출력
               messageWait("160|"+ getRoomInfo());
               messageWait("180|"+ getWaitInwon());
               break;
              
            case "155": //이름변경 (추가된부분)
                newname=msgs[2];
//                messageWait("155|" + newname);
                nickName = newname;
                messageWait("180|" +  newgetInwon(nickName1,newname));
               break;
               
            case "160": //방만들기(대화방 입장)               
               myRoom = new Room();
                myRoom.title =msgs[1];//방제목
                myRoom.count = 1;
//                myRoom.boss = nickName;
                
                roomV.add(myRoom); //대기실 --> 대화방 이동
                waitV.remove(this);
                myRoom.userV.add(this);
               
               messageRoom("200|"+nickName);//방인원에게 입장 알림
               //대기실 사용자들에게 방정보를 출력 
               messageWait("160|"+ getRoomInfo());
               messageWait("180|"+ getWaitInwon());
               break;
               
            case "170": //대기실에서 대화방 인원정보
               messageTo("170|"+getRoomInwon(msgs[1]));
               break;
               
            case "175": //대화방에서 대화방 인원정보
               messageRoom("175|"+getRoomInwon());
               break;
            case "177":
            	messageRoom("175|"+newgetRoomInwon(nickName1, newname));
            	break;
               
            case "180": //대기실 인원정보
               nickName=msgs[1];
               messageWait("160|"+getRoomInfo());
               messageWait("180|"+ getWaitInwon());
               break;
               
            case "200": //방들어가기 (대화방 입장) ----> msgs[] = {"200","자바방")
               for(int i=0; i<roomV.size(); i++){//방이름 찾기!!
                  Room r = roomV.get(i);
                  if(r.title.equals(msgs[1])){//일치하는 방 찾음
                     myRoom = r;
                     myRoom.count++;//인원수 1증가
                     break;
                     }
                  }//for

               //대기실 ----> 대화방 이동
               waitV.remove(this);
               myRoom.userV.add(this);   
               
               messageRoom("200|"+nickName);//방인원에게 입장 알림  
               
               //들어갈 방의 title전달
               messageTo("202|" + myRoom.title);
               messageWait("160|" + getRoomInfo()); // 자바-1 , 보노보노-2
               messageWait("180|" + getWaitInwon());
               break;

            case "300": // 메시지
               messageRoom("300|[" + nickName + "]▶ " + msgs[1]);
               // 클라이언트에게 메시지 보내기
               break;
            
            case "400": // 대화방 퇴장
               myRoom.count--;// 인원수 감소
               messageRoom("400|" + nickName);// 방인원들에게 퇴장 알림

               // 대화방 ----> 대기실 이동
               myRoom.userV.remove(this);
               waitV.add(this);
               
               //추가
               messageWait("180|" + getWaitInwon());
               // 대화방 퇴장후 방인원 다시출력
               messageRoom("175|" + getRoomInwon());

               // 대기실에 방정보 다시출력
               messageWait("160|" + getRoomInfo());
               break;
               
            case "450":
                myRoom.count--;// 인원수 감소
                messageRoom("400|" + nickName);// 방인원들에게 퇴장 알림

                // 대화방 ----> 대기실 이동
                myRoom.userV.remove(this);
                waitV.add(this);
                
                // 대화방 퇴장후 방인원 다시출력
                messageRoom("175|" + newgetInwon(nickName1,newname));
                messageWait("180|" + newgetInwon(nickName1, newname));

                // 대기실에 방정보 다시출력
                messageWait("160|" + getRoomInfo());
            	
            	break;
            }
         }
      }
   } catch (IOException e) {
      System.out.println("★");
      e.printStackTrace();
      }
   }
   
   public String newgetInwon(String n, String m) { //추가
      String str = "";
      for (int i = 0; i < allV.size(); i++) {
         Service s = allV.get(i);
         if(s.nickName1.equals(n)) {
        	 s.nickName1 = m;
         }
    	 allV.setElementAt(s, i);
      }
      
      
      for(int i=0;i<waitV.size();i++) {
	        Service s =waitV.get(i);
	        if(s.nickName1.equals(n)) {
	       	 s.nickName1 = m;
	        }
	        waitV.setElementAt(s, i);
      }
	  for(int i=0;i<waitV.size();i++) {
		  Service s1= waitV.get(i);
		  str+=s1.nickName;
		  if(i < waitV.size()-1)
			  str+=",";
      }
      System.out.print("꾸"+str);
      return str;
   }
     
   
   public String getRoomInfo() { //방이름과 인원수
      String str = "";
      for (int i = 0; i < roomV.size(); i++) {
         Room r = roomV.get(i);
         str += r.title + "--" + r.count;
         if (i < roomV.size() - 1)
            str += ",";
      }
      return str;
   }
   
   public String newgetRoomInwon(String n,String m) {
	      String str = "";
	      for(int i=0;i<myRoom.userV.size();i++) {
		        Service s =myRoom.userV.get(i);
		        if(s.nickName1.equals(n)) {
		       	 s.nickName1 = m;
		        }
		        myRoom.userV.setElementAt(s, i);
	      }
	      for (int i = 0; i < myRoom.userV.size(); i++) {
	         Service ser = myRoom.userV.get(i);
	         str += ser.nickName;
	         if (i < myRoom.userV.size() - 1)
	            str += ",";
	      }
	      System.out.println("가튼뱡:"+str);
	      return str;
	   
   }
   public String getRoomInwon() {// 같은방의 인원정보
      String str = "";
      for (int i = 0; i < myRoom.userV.size(); i++) {
         Service ser = myRoom.userV.get(i);
         str += ser.nickName;
         if (i < myRoom.userV.size() - 1)
            str += ",";
      }
      return str;
   }
   
   public String getRoomInwon(String title) {// 방제목 클릭시 방의 인원정보
      String str = "";
      for (int i = 0; i < roomV.size(); i++) {
         Room room = roomV.get(i);
         if (room.title.equals(title)) {
            for (int j = 0; j < room.userV.size(); j++) {
               Service ser = room.userV.get(j);
               str += ser.nickName;
               if (j < room.userV.size() - 1)
                  str += ",";
            }
            break;
         }
      }
      return str;
   }
   
   public String getWaitInwon() {
      String str = "";
      for (int i = 0; i < waitV.size(); i++) {
         Service ser = waitV.get(i);
         str += ser.nickName;
         if (i < waitV.size() - 1)
            str += ",";
      }
      return str;
   }
   
   public void messageAll(String msg) {// 전체사용자
      // 접속된 모든 클라이언트(대기실+대화방)에게 메시지 전달
      for (int i = 0; i < allV.size(); i++) {
         Service service = allV.get(i); // 각각의 클라이언트 얻어오기
         try {
            service.messageTo(msg);
         } catch (IOException e) {
            // 에러발생 ---> 클라이언트 접속 끊음
            allV.remove(i--); // 접속 끊긴 클라이언트를 벡터에서 삭제
            System.out.println("클라이언트 접속 끊음!!");
         }
      }
   }
   
   public void messageWait(String msg) {// 대기실 사용자
      for (int i = 0; i < waitV.size(); i++) {
         Service service = waitV.get(i); // 각각의 클라이언트 얻어오기
         try {
            service.messageTo(msg);
         } catch (IOException e) {
            // 에러발생 ---> 클라이언트 접속 끊음
            waitV.remove(i--); // 접속 끊긴 클라이언트를 벡터에서 삭제
            System.out.println("클라이언트 접속 끊음!!");
         }
      }
   }
   

   
   public void messageRoom(String msg) {// 대화방사용자
      for (int i = 0; i < myRoom.userV.size(); i++) {
         Service service = myRoom.userV.get(i); // 각각의 클라이언트 얻어오기
         try {
            service.messageTo(msg);
         } catch (IOException e) {
            // 에러발생 ---> 클라이언트 접속 끊음
            myRoom.userV.remove(i--); // 접속 끊긴 클라이언트를 벡터에서 삭제
            System.out.println("클라이언트 접속 끊음!!");
         }
      }
   }// messageAll
   
   public void messageTo(String msg) throws IOException {
   
      // 특정 클라이언트에게 메시지 전달 (실제 서버--->클라이언트 메시지 전달)
      out.write((msg + "\n").getBytes());
   
   }
}