package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import nio.NioSocketServer;

public class MainUI extends JFrame {
    // �õ���ʾ����Ļ�Ŀ��
    private int width = Toolkit.getDefaultToolkit().getScreenSize().width;
    private int height = Toolkit.getDefaultToolkit().getScreenSize().height;
    // ���崰��Ŀ��
    private int windowsWedth = 800;
    private int windowsHeight = 800;
    
    public MainUI() {
		/**
		   *   ����lookandfeel (Ƥ��)
		 */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		Font f = new Font("����", Font.PLAIN, 13);
		UIManager.put("Menu.font", f);
		init();
    }
    
	/**
	 *  ��ʼ��
	 */
	private void init() {
		this.setTitle("TWord");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
        // ���ô���λ�úʹ�С
        this.setBounds((width - windowsWedth) / 2,
                (height - windowsHeight) / 2, windowsWedth, windowsHeight);
        
        this.setLayout(new BorderLayout());
        JPanel topJPanel = new JPanel();
        this.add(topJPanel,BorderLayout.NORTH);
        JTextArea centerJTextArea = new JTextArea();
        this.add(centerJTextArea,BorderLayout.CENTER);
        
        //-------------------topJPanel����-----------------------------------------------
        topJPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton sartServerButton = new JButton("����������");
        sartServerButton.setFont(new Font("����", Font.PLAIN, 20));
        topJPanel.add(sartServerButton);
        
        JButton sartERPdatas = new JButton("����ERP");
        sartERPdatas.setFont(new Font("����", Font.PLAIN, 20));
        topJPanel.add(sartERPdatas);
        
        JLabel label = new JLabel("�ļ�·����");
        label.setFont(new Font("����", Font.PLAIN, 16));
        topJPanel.add(label);
        
        JTextField text = new JTextField(30);
        text.setFont(new Font("����", Font.PLAIN, 16));
        text.setText("d:/tword/erp/datas");
        topJPanel.add(text);
        //--------------------------------------------------------------------------------
               
        sartServerButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				sartServerButton.setEnabled(false);
				NioSocketServer server = new NioSocketServer();
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						server.start();
					}
				}).start();
			}
		});
        
        this.setVisible(true);
	}

}
