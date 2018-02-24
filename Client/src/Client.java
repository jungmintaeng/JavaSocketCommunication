import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
	private String Ip;	//������ ip
	private int PORT = 8080;

	private Socket socket;
	private InputStream input;
	private OutputStream output;
	private DataInputStream datainput;
	private DataOutputStream dataoutput;

	public Client(String ip){	//�����ڸ� ȣ���ص� �ٷ� ������ ����ǵ��� ��(���� : ����ip)
		try{
			Ip = ip;
			socket = new Socket(Ip, PORT);	//������ ���� �õ�
			if(socket != null)
			{
				System.out.println("\nConnected to Server\n");	//���� ������ �޽��� ���
			}
			
			Thread thread = new Thread(new Runnable(){		//�������� �� ���� ����� �޴� ������
				public void run(){
					while(true){
						try{
							input = socket.getInputStream();
							datainput = new DataInputStream(input);	//����� ������ �Ҵ�
							
							String msg = datainput.readUTF();	//���� ����� ������
							if(msg.compareTo("") == 0)	//�� ���ڿ��� �� Empty Set���� ����ϰ� 
								System.out.println("===---------------------------===Result\nEmpty Set\n");
							else						//�� ���ڿ��� �ƴϸ� ���� ����� ����Ѵ�.
								System.out.println("===---------------------------===Result\n" + msg + "\n");
						}
						catch(IOException ex)	//���� �߻��� ���ҽ� ����
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

			thread.start();	//�����带 �����ϰ�
			SelectQuery();	//� ���Ǹ� ������ ������ a or b or c�� �Է���
		}
		catch(IOException ex)
		{
			System.out.println("\n\nCONNECT ERROR\n\n");
		}
		catch(Exception e){}
	}


	private void SelectQuery()	//� ���Ǹ� ������ ������ �Է�
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
		while(SelectedQuery != "a" && SelectedQuery != "c");	//�� �� �ƴ϶�� �ٽ� ����
	}

	private void SendQuery(){	//���� a�� ���� c�� �� ���� ����
		System.out.println("Input Query");

		Scanner sc = new Scanner(System.in);
		String msg = sc.nextLine();

		try
		{
			output = socket.getOutputStream();
			dataoutput = new DataOutputStream(output);//��Ʈ�� �Ҵ�

			dataoutput.writeUTF(msg);	//������ ����(�޽���) ����
		}
		catch(IOException ex)
		{
			System.out.println("SEND ERROR");
		}
	}

	private void SendBQuery(){		//���� b�� ��(��Ÿ�� ���� �ƴ�)
		System.out.println("B Query\nSELECT Fname, Lname\n"
				+ "FROM EMPLOYEE, DEPENDENT\n"
				+ "WHERE Ssn = Essn AND Fname = Dependent_name");
		
		try
		{
			output = socket.getOutputStream();
			dataoutput = new DataOutputStream(output);

			dataoutput.writeUTF("QueryB");	//�׳� QueryB��� ������ �������� ����b��� ó���ϵ��� ��
		}
		catch(IOException ex)
		{
			System.out.println("SEND ERROR");
		}
	}
}
