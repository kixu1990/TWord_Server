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
    // 得到显示器屏幕的宽高
    private int width = Toolkit.getDefaultToolkit().getScreenSize().width;
    private int height = Toolkit.getDefaultToolkit().getScreenSize().height;
    // 定义窗体的宽高
    private int windowsWedth = 800;
    private int windowsHeight = 800;
    
    public MainUI() {
		/**
		   *   设置lookandfeel (皮肤)
		 */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		Font f = new Font("黑体", Font.PLAIN, 13);
		UIManager.put("Menu.font", f);
		init();
    }
    
	/**
	 *  初始化
	 */
	private void init() {
		this.setTitle("TWord");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
        // 设置窗体位置和大小
        this.setBounds((width - windowsWedth) / 2,
                (height - windowsHeight) / 2, windowsWedth, windowsHeight);
        
        this.setLayout(new BorderLayout());
        JPanel topJPanel = new JPanel();
        this.add(topJPanel,BorderLayout.NORTH);
        JTextArea centerJTextArea = new JTextArea();
        this.add(centerJTextArea,BorderLayout.CENTER);
        
        //-------------------topJPanel设置-----------------------------------------------
        topJPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton sartServerButton = new JButton("开启服务器");
        sartServerButton.setFont(new Font("宋体", Font.PLAIN, 20));
        topJPanel.add(sartServerButton);
        
        JButton sartERPdatas = new JButton("接收ERP");
        sartERPdatas.setFont(new Font("宋体", Font.PLAIN, 20));
        topJPanel.add(sartERPdatas);
        
        JLabel label = new JLabel("文件路径：");
        label.setFont(new Font("宋体", Font.PLAIN, 16));
        topJPanel.add(label);
        
        JTextField text = new JTextField(30);
        text.setFont(new Font("宋体", Font.PLAIN, 16));
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
