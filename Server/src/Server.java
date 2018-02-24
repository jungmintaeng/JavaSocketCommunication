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

	private ServerSocket socket;	//������ ������ ������ ����
	private Socket accsocket;		//���ӵ� Ŭ���̾�Ʈ ����
	private int PORT = 8080;		//��Ʈ��ȣ
	private InputStream input;
	private OutputStream output;
	private DataInputStream datainput;
	private DataOutputStream dataoutput;	//����� ������ ��Ʈ��

	java.sql.Statement st = null;
	ResultSet rs = null;
	Connection con = null;

	public void ServerStart() throws SQLException	//jdbc�� ���� mysql�� �����ϰ� ������ ������
	{
		try{
			try
			{
				con = DriverManager.getConnection(
						"jdbc:mysql://localhost", "root", "1234");	//��ġ ����(DB ����)
			}
			catch(SQLException ex)
			{
				System.out.println("SQL ���� ����!\n");	//���н� �����޽��� ���
				return;
			}

			st = con.createStatement();
			rs = st.executeQuery("USE COMPANY");		//����� DB�� �����ϰ�

			if(st.execute("show tables"))				//COMPANY �ȿ� �ִ� Table ����
				rs = st.getResultSet();

			System.out.println("------------Tables in COMPANY DB------------");
			while(rs.next())		//������ ���� Table ���
			{
				System.out.println(rs.getNString(1));
			}
			
			System.out.println("------------------------------");

			socket = new ServerSocket(PORT);		//���� ����
			if(socket != null)
			{
				System.out.println("Server opened\n");
				Connect();							//Ŭ���̾�Ʈ�� ��ٸ��� �޼ҵ�
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
	//ServerStart �޼ҵ� �ȿ��� ������� Client�� accept ���ִ� �޼ҵ�
	{
		Thread thread = new Thread(new Runnable(){
			public void run()
			{
				while(true)
				{
					try{
						accsocket = socket.accept();	//Ŭ���̾�Ʈ ���ӱ��� ���
						System.out.println("User Connected\n"); //���ӵǸ� �޼��� ���

						input = accsocket.getInputStream();
						datainput = new DataInputStream(input);

						output = accsocket.getOutputStream();
						dataoutput = new DataOutputStream(output); //����� ��Ʈ�� �Ҵ�

						Receive();	//�޴� ������ �޼ҵ� ����
					}
					catch(IOException ex)	//IO ���� �߻��� �����޽��� ���
					{
						System.out.println("ACCEPT ERROR\n");
					}
					catch (SQLException ex)	//SQL ���� ���� �߻��� �����޼��� ���
					{
						System.out.println("SQL ERROR\n");
					}
				}
			}
		});

		thread.start();
	}

	private String[] ProcessString(String msg)
	{//Ŭ���̾�Ʈ���� ���� ���ڿ��� ���Ƿ� ó���ϱ� ���� �����̳� ���ڸ� �����ϴ� �޼ҵ�
		String[] sp = msg.split("', '");	//', '�� �������� ��
		if(sp.length == 3)	//string ����� 3���� ������ ���� a
		{
			sp[0] = sp[0].substring(1);
			sp[2] = sp[2].substring(0, sp[2].length()-1);	//' ����
			
			String[] dno = sp[0].split(" ");
			String[] hour = sp[1].split(" ");
			String[] pname = sp[2].split(" ");	//������ �������� ���ڿ� ����
			
			sp[0] = dno[1];	//ex)department 5�� dno[0] = "department", dno[1] = "5"�� ��������
							//���ǿ� �ʿ��� ���� 5�� return�ϱ� ���� �Ҵ�
			sp[1] = hour[0]; //ex)10 Hours�� dno[0] = "10", dno[1] = "Hours"�� ��������
							 //���ǿ� �ʿ��� ���� 10�� return�ϱ� ���� �Ҵ�
			sp[2] = pname[0] + pname[1];	//ex)Product X�� �ѷ� ������ �ϳ��� ���ļ� �߰��� ���� ����
		}
		else if(sp.length == 1)//', '�� ���� ���ڿ��� 1������ Ŭ���̾�Ʈ���� ��û�� ���Ǵ� b�̰ų� c
		{
			sp = sp[0].split(" ");	//������ �������� ���ڿ��� ������.
			if(sp[0].compareTo("QueryB") == 0)
			{//���� ��û�� ������ b���, ���ڿ��� ������ �ʿ� ���� �ٷ� return�Ѵ�.
				return sp;
			}
			else
			{//���� c���
				sp[0] = sp[0].substring(1, sp[0].length()); 
				//ex) 'Franklin Wong'�̶�� sp[0] = "'Franklin",
				sp[1] = sp[1].substring(0, sp[1].length()-1);
				//sp[1] = "Wong'"�� ����Ǿ� ���� ���̹Ƿ� ' ����
			}
		}
		else{}
				
		return sp;	//������ ���ڿ��� return
	}
	
	private void Receive() throws SQLException	//��û�� �޾��� �� ���Ǹ� �����ϴ� �޼ҵ�
	{
		Thread thread = new Thread(new Runnable(){
			public void run(){
				while(true)
				{
					try{
						String sendstring = "";				//���� ����� ������ ���ڿ�
						String msg = datainput.readUTF();	//Ŭ���̾�Ʈ�κ��� �޽����� ������
						System.out.println("Request : " + msg + "\n-----------------------==Result");
						
						String[] sp = ProcessString(msg);
						
						if(sp.length == 3)//query a
						{
							PreparedStatement p = con.prepareStatement("select distinct Fname, Lname "
									+ "from employee, works_on, project "
									+ "where (Ssn = Essn) AND (Pnumber = Pno) "
									+ "AND (Dnum = ?) AND (Hours >= ?) AND (Pname = ?)");
											//���ǿ��� ����ڿ��� �Է¹��� �κ��� ?�� �ۼ�
							p.clearParameters();
							p.setInt(1, Integer.parseInt(sp[0]));
							p.setInt(2, Integer.parseInt(sp[1]));
							p.setString(3, sp[2]);						//?�� �� ���ڿ� or ���� ����
							
							rs = p.executeQuery();						//���� ����
							
							while(rs.next())//����� �ݺ��� ���� sendstring�� ����
							{
								String Fname, Lname;
								Fname = rs.getString(1);
								Lname = rs.getString(2);
								sendstring = sendstring + Fname + " " + Lname + "\n";
								
								System.out.println(Fname + " " + Lname);
							}	
							if(sendstring.compareTo("") == 0)
								System.out.println("Empty Set");	//sendstring�� ""��� empty set ���
							
							Send_Result(sendstring);	//client�� ���� ��� ������
						}
						else if(sp.length == 2)	//query C
						{
							PreparedStatement p = con.prepareStatement("select Fname, Lname "
									+ "from employee e "
									+ "where e.Super_ssn = "
															+ "(select Ssn "//subquery
															+ "from employee s "
															+ "where s.Fname = ? AND s.Lname = ?)");
							p.clearParameters();					//����ڿ��� �̸��� �Է¹����Ƿ� ?�� �ۼ�
							p.setString(1, sp[0]);
							p.setString(2, sp[1]);		//����ڿ��� �Է¹��� �̸��� ���ǿ� �������� ��
							
							rs = p.executeQuery();		//���� ����
							
							while(rs.next())//����� �ݺ��� ���� sendstring�� ����
							{
								String Fname, Lname;
								Fname = rs.getString(1);
								Lname = rs.getString(2);
								sendstring = sendstring + Fname + " " + Lname + "\n";
								
								System.out.println(Fname + " " + Lname);
							}
							
							
							if(sendstring.compareTo("") == 0)  //sendstring�� ""��� empty set ���
								System.out.println("Empty Set");
							
							Send_Result(sendstring);		//client�� ���� ��� ������
						}
						else if(sp.length == 1 && sp[0].compareTo("QueryB") == 0)//query B
						{
							PreparedStatement p = con.prepareStatement("select Fname, Lname"
									+ " from employee, dependent"
									+ " where Ssn = Essn AND Fname = Dependent_name");
							//�̸� preparedstatement�� ���� �ۼ� ��
							rs = p.executeQuery();	//���� ����
							
							while(rs.next())//����� �ݺ��� ���� sendstring�� ����
							{
								String Fname, Lname;
								Fname = rs.getString(1);
								Lname = rs.getString(2);
								sendstring = sendstring + Fname + " " + Lname + "\n";
								
								System.out.println(Fname + " " + Lname);
							}
							if(sendstring.compareTo("") == 0)	//sendstring�� ""��� empty set ���
								System.out.println("Empty Set");
							
							Send_Result(sendstring);		//client�� ���� ��� ������
						}
					}
					catch (IOException ex)
					{
						try{
							datainput.close();
							dataoutput.close();
							accsocket.close();	//���ҽ� �Ҵ� ����
							break;
						}
						catch (Exception e){}
					}
					catch (SQLException e)
					{
						System.out.println("SQL ERROR\n\n");	//���� ������ sql error
					}
				}
			}
		});

		thread.start();
	}

	private void Send_Result(String str)	//Ŭ���̾�Ʈ�� ���ڿ��� �����ִ� �Լ�
	{
		try{
			dataoutput.writeUTF(str);	//Ŭ���̾�Ʈ�� ���ڿ� ����
		}
		catch(IOException e)
		{
			System.out.println("Query Send ERROR\n");
		}
	}
}
