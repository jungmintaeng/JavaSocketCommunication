import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Server {

	private ServerSocket socket;	//서버의 소켓을 저장할 변수
	private Socket accsocket;		//접속된 클라이언트 소켓
	private int PORT = 8080;		//포트번호
	private InputStream input;
	private OutputStream output;
	private DataInputStream datainput;
	private DataOutputStream dataoutput;	//통신할 데이터 스트림

	java.sql.Statement st = null;
	ResultSet rs = null;
	Connection con = null;

	public void ServerStart() throws SQLException	//jdbc를 통해 mysql과 연동하고 서버를 오픈함
	{
		try{
			try
			{
				con = DriverManager.getConnection(
						"jdbc:mysql://localhost", "root", "1234");	//장치 설정(DB 연동)
			}
			catch(SQLException ex)
			{
				System.out.println("SQL 연결 실패!\n");	//실패시 에러메시지 출력
				return;
			}

			st = con.createStatement();
			rs = st.executeQuery("USE COMPANY");		//사용할 DB를 설정하고

			if(st.execute("show tables"))				//COMPANY 안에 있는 Table 얻어옴
				rs = st.getResultSet();

			System.out.println("------------Tables in COMPANY DB------------");
			while(rs.next())		//루프를 돌며 Table 출력
			{
				System.out.println(rs.getNString(1));
			}
			
			System.out.println("------------------------------");

			socket = new ServerSocket(PORT);		//소켓 생성
			if(socket != null)
			{
				System.out.println("Server opened\n");
				Connect();							//클라이언트를 기다리는 메소드
			}

		}
		catch(IOException ex)
		{
			System.out.println("SOCKET ERROR\n");
		}
		catch (SQLException e)
		{
			System.out.println("SQL ERROR\n");
		}
		catch (Exception eee){}
	}

	private void Connect() throws SQLException	
	//ServerStart 메소드 안에서 쓰레드로 Client를 accept 해주는 메소드
	{
		Thread thread = new Thread(new Runnable(){
			public void run()
			{
				while(true)
				{
					try{
						accsocket = socket.accept();	//클라이언트 접속까지 대기
						System.out.println("User Connected\n"); //접속되면 메세지 출력

						input = accsocket.getInputStream();
						datainput = new DataInputStream(input);

						output = accsocket.getOutputStream();
						dataoutput = new DataOutputStream(output); //통신할 스트림 할당

						Receive();	//받는 쓰레드 메소드 실행
					}
					catch(IOException ex)	//IO 예외 발생시 에러메시지 출력
					{
						System.out.println("ACCEPT ERROR\n");
					}
					catch (SQLException ex)	//SQL 관련 오류 발생시 에러메세지 출력
					{
						System.out.println("SQL ERROR\n");
					}
				}
			}
		});

		thread.start();
	}

	private String[] ProcessString(String msg)
	{//클라이언트에서 보낸 문자열을 질의로 처리하기 위해 공백이나 문자를 제거하는 메소드
		String[] sp = msg.split("', '");	//', '로 나누었을 때
		if(sp.length == 3)	//string 덩어리가 3개가 나오면 질의 a
		{
			sp[0] = sp[0].substring(1);
			sp[2] = sp[2].substring(0, sp[2].length()-1);	//' 제거
			
			String[] dno = sp[0].split(" ");
			String[] hour = sp[1].split(" ");
			String[] pname = sp[2].split(" ");	//공백을 기준으로 문자열 나눔
			
			sp[0] = dno[1];	//ex)department 5를 dno[0] = "department", dno[1] = "5"로 나눴으니
							//질의에 필요한 값인 5를 return하기 위해 할당
			sp[1] = hour[0]; //ex)10 Hours를 dno[0] = "10", dno[1] = "Hours"로 나눴으니
							 //질의에 필요한 값인 10을 return하기 위해 할당
			sp[2] = pname[0] + pname[1];	//ex)Product X를 둘로 나누고 하나로 합쳐서 중간의 공백 제거
		}
		else if(sp.length == 1)//', '로 나눈 문자열이 1덩어리라면 클라이언트에서 요청한 질의는 b이거나 c
		{
			sp = sp[0].split(" ");	//공백을 기준으로 문자열을 나눈다.
			if(sp[0].compareTo("QueryB") == 0)
			{//그중 요청한 쿼리가 b라면, 문자열을 가공할 필요 없이 바로 return한다.
				return sp;
			}
			else
			{//쿼리 c라면
				sp[0] = sp[0].substring(1, sp[0].length()); 
				//ex) 'Franklin Wong'이라면 sp[0] = "'Franklin",
				sp[1] = sp[1].substring(0, sp[1].length()-1);
				//sp[1] = "Wong'"이 저장되어 있을 것이므로 ' 제거
			}
		}
		else{}
				
		return sp;	//가공된 문자열을 return
	}
	
	private void Receive() throws SQLException	//요청을 받았을 때 질의를 수행하는 메소드
	{
		Thread thread = new Thread(new Runnable(){
			public void run(){
				while(true)
				{
					try{
						String sendstring = "";				//질의 결과를 저장할 문자열
						String msg = datainput.readUTF();	//클라이언트로부터 메시지를 받으면
						System.out.println("Request : " + msg + "\n-----------------------==Result");
						
						String[] sp = ProcessString(msg);
						
						if(sp.length == 3)//query a
						{
							PreparedStatement p = con.prepareStatement("select distinct Fname, Lname "
									+ "from employee, works_on, project "
									+ "where (Ssn = Essn) AND (Pnumber = Pno) "
									+ "AND (Dnum = ?) AND (Hours >= ?) AND (Pname = ?)");
											//질의에서 사용자에게 입력받을 부분을 ?로 작성
							p.clearParameters();
							p.setInt(1, Integer.parseInt(sp[0]));
							p.setInt(2, Integer.parseInt(sp[1]));
							p.setString(3, sp[2]);						//?에 들어갈 문자열 or 숫자 설정
							
							rs = p.executeQuery();						//질의 수행
							
							while(rs.next())//결과를 반복을 통해 sendstring에 저장
							{
								String Fname, Lname;
								Fname = rs.getString(1);
								Lname = rs.getString(2);
								sendstring = sendstring + Fname + " " + Lname + "\n";
								
								System.out.println(Fname + " " + Lname);
							}	
							if(sendstring.compareTo("") == 0)
								System.out.println("Empty Set");	//sendstring이 ""라면 empty set 출력
							
							Send_Result(sendstring);	//client에 질의 결과 보내줌
						}
						else if(sp.length == 2)	//query C
						{
							PreparedStatement p = con.prepareStatement("select Fname, Lname "
									+ "from employee e "
									+ "where e.Super_ssn = "
															+ "(select Ssn "//subquery
															+ "from employee s "
															+ "where s.Fname = ? AND s.Lname = ?)");
							p.clearParameters();					//사용자에게 이름을 입력받으므로 ?로 작성
							p.setString(1, sp[0]);
							p.setString(2, sp[1]);		//사용자에게 입력받은 이름을 질의에 적용해준 후
							
							rs = p.executeQuery();		//질의 수행
							
							while(rs.next())//결과를 반복을 통해 sendstring에 저장
							{
								String Fname, Lname;
								Fname = rs.getString(1);
								Lname = rs.getString(2);
								sendstring = sendstring + Fname + " " + Lname + "\n";
								
								System.out.println(Fname + " " + Lname);
							}
							
							
							if(sendstring.compareTo("") == 0)  //sendstring이 ""라면 empty set 출력
								System.out.println("Empty Set");
							
							Send_Result(sendstring);		//client에 질의 결과 보내줌
						}
						else if(sp.length == 1 && sp[0].compareTo("QueryB") == 0)//query B
						{
							PreparedStatement p = con.prepareStatement("select Fname, Lname"
									+ " from employee, dependent"
									+ " where Ssn = Essn AND Fname = Dependent_name");
							//미리 preparedstatement에 질의 작성 후
							rs = p.executeQuery();	//질의 수행
							
							while(rs.next())//결과를 반복을 통해 sendstring에 저장
							{
								String Fname, Lname;
								Fname = rs.getString(1);
								Lname = rs.getString(2);
								sendstring = sendstring + Fname + " " + Lname + "\n";
								
								System.out.println(Fname + " " + Lname);
							}
							if(sendstring.compareTo("") == 0)	//sendstring이 ""라면 empty set 출력
								System.out.println("Empty Set");
							
							Send_Result(sendstring);		//client에 질의 결과 보내줌
						}
					}
					catch (IOException ex)
					{
						try{
							datainput.close();
							dataoutput.close();
							accsocket.close();	//리소스 할당 해제
							break;
						}
						catch (Exception e){}
					}
					catch (SQLException e)
					{
						System.out.println("SQL ERROR\n\n");	//질의 오류시 sql error
					}
				}
			}
		});

		thread.start();
	}

	private void Send_Result(String str)	//클라이언트에 문자열을 보내주는 함수
	{
		try{
			dataoutput.writeUTF(str);	//클라이언트에 문자열 전송
		}
		catch(IOException e)
		{
			System.out.println("Query Send ERROR\n");
		}
	}
}
