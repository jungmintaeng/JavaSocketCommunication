import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
	private String Ip;	//서버의 ip
	private int PORT = 8080;

	private Socket socket;
	private InputStream input;
	private OutputStream output;
	private DataInputStream datainput;
	private DataOutputStream dataoutput;

	public Client(String ip){	//생성자만 호출해도 바로 서버에 연결되도록 함(인자 : 서버ip)
		try{
			Ip = ip;
			socket = new Socket(Ip, PORT);	//서버에 접속 시도
			if(socket != null)
			{
				System.out.println("\nConnected to Server\n");	//접속 성공시 메시지 출력
			}
			
			Thread thread = new Thread(new Runnable(){		//서버에서 온 질의 결과를 받는 쓰레드
				public void run(){
					while(true){
						try{
							input = socket.getInputStream();
							datainput = new DataInputStream(input);	//통신할 쓰레드 할당
							
							String msg = datainput.readUTF();	//질의 결과를 받으면
							if(msg.compareTo("") == 0)	//빈 문자열일 때 Empty Set임을 출력하고 
								System.out.println("===---------------------------===Result\nEmpty Set\n");
							else						//빈 문자열이 아니면 질의 결과를 출력한다.
								System.out.println("===---------------------------===Result\n" + msg + "\n");
						}
						catch(IOException ex)	//예외 발생시 리소스 해제
						{
							try{
								System.out.println("RECEIVE ERROR\n\n");
								output.close();
								input.close();
								dataoutput.close();
								datainput.close();
								socket.close();
								break;
							}
							catch(Exception e){
								System.out.println("STRANGE ERROR\n\n");
							}
						}
					}
				}
			});

			thread.start();	//쓰레드를 시작하고
			SelectQuery();	//어떤 질의를 수행할 것인지 a or b or c를 입력함
		}
		catch(IOException ex)
		{
			System.out.println("\n\nCONNECT ERROR\n\n");
		}
		catch(Exception e){}
	}


	private void SelectQuery()	//어떤 질의를 수행할 것인지 입력
	{
		Scanner sc;
		String SelectedQuery;

		do
		{
			sc = new Scanner(System.in);

			SelectedQuery = sc.nextLine();

			if(SelectedQuery.compareTo("a") == 0)	//a
			{
				SendQuery();
			}
			else if(SelectedQuery.compareTo("b") == 0)	//b
			{
				SendBQuery();
			}
			else if(SelectedQuery.compareTo("c") == 0)	//c
			{
				SendQuery();
			}
		}
		while(SelectedQuery != "a" && SelectedQuery != "c");	//셋 다 아니라면 다시 선택
	}

	private void SendQuery(){	//질의 a나 질의 c일 때 쿼리 전송
		System.out.println("Input Query");

		Scanner sc = new Scanner(System.in);
		String msg = sc.nextLine();

		try
		{
			output = socket.getOutputStream();
			dataoutput = new DataOutputStream(output);//스트림 할당

			dataoutput.writeUTF(msg);	//서버에 질의(메시지) 전송
		}
		catch(IOException ex)
		{
			System.out.println("SEND ERROR");
		}
	}

	private void SendBQuery(){		//질의 b일 때(런타임 쿼리 아님)
		System.out.println("B Query\nSELECT Fname, Lname\n"
				+ "FROM EMPLOYEE, DEPENDENT\n"
				+ "WHERE Ssn = Essn AND Fname = Dependent_name");
		
		try
		{
			output = socket.getOutputStream();
			dataoutput = new DataOutputStream(output);

			dataoutput.writeUTF("QueryB");	//그냥 QueryB라고 보내면 서버에서 질의b라고 처리하도록 함
		}
		catch(IOException ex)
		{
			System.out.println("SEND ERROR");
		}
	}
}
