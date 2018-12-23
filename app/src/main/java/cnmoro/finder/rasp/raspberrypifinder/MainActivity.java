package cnmoro.finder.rasp.raspberrypifinder;

import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

  final int PORT = 8089;
  String RASP_ADDRESS = "";
  String LOCAL_NETWORK = "";
  String LOCAL_ADDRESS = "";
  final int THREAD_SLEEP_START_MS = 1000;
  TextView txt_raspip;
  final String gitUrl = "https://github.com/cnmoro/Raspberry-Pi-DHCP-Finder";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    txt_raspip = findViewById(R.id.txt_raspip);

    new socketServer().start();
  }

  public void openGitLink(View v) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(gitUrl));
    startActivity(browserIntent);
  }

  public void searchForRasp(View v) {
    RASP_ADDRESS = "";
    new CountDownTimer(6000, 1000) {
      public void onTick(long millisUntilFinished) {
      }

      public void onFinish() {
        txt_raspip.setText("Raspberry not found in " + LOCAL_NETWORK + "X");
      }
    }.start();
    txt_raspip.setText("Searching for Raspberry Pi ...");
    new raspAddrManager().start();
  }

  class socketServer extends Thread {

    ServerSocket server;

    public socketServer() {
      try {
        this.server = new ServerSocket(PORT);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void run() {
      while (true) {
        try {
          Socket socket = this.server.accept();
          socket.getRemoteSocketAddress().toString();
          InputStreamReader isr = new InputStreamReader(socket.getInputStream());
          BufferedReader br = new BufferedReader(isr);

          String msg = br.readLine();
          if (msg != null) {
            if (msg.equals("rasp!")) {
              String addr = socket.getRemoteSocketAddress().toString();
              addr = addr.replace("/", "");
              addr = addr.split("\\:")[0];

              System.out.println(msg);
              RASP_ADDRESS = addr;

              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  txt_raspip.setText("Raspberry Pi Address is: " + RASP_ADDRESS);
                }
              });
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  class raspAddrManager extends Thread {

    public raspAddrManager() {
    }

    public void run() {
      try {
        String localAddress = getLocalAddr();
        LOCAL_ADDRESS = localAddress;

        String addrParts[] = localAddress.split("\\.");

        if (addrParts.length == 4) {
          String addrProto = addrParts[0] + "." + addrParts[1] + "." + addrParts[2] + ".";
          LOCAL_NETWORK = addrProto;

          for (int i = 1; i <= 255; i++) {
            if (!(addrProto + i).equals(localAddress)) {
              new raspSocketFinder(addrProto + i).start();
            }
          }

        }

      } catch (Exception e) {
      }
    }

  }

  class raspSocketFinder extends Thread {

    Socket socket;
    String ipAddr;

    public raspSocketFinder(String ipAddr) {
      try {
        this.ipAddr = ipAddr;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void run() {
      try {
        Thread.sleep(THREAD_SLEEP_START_MS);

        this.socket = new Socket(ipAddr, PORT);
        OutputStream output = socket.getOutputStream();
        output.write("rasp?".getBytes());
      } catch (Exception e) {
      }
    }
  }

  public static String getLocalAddr() {
    String ip = "";
    try {
      Enumeration e = NetworkInterface.getNetworkInterfaces();
      while (e.hasMoreElements()) {
        NetworkInterface n = (NetworkInterface) e.nextElement();
        Enumeration ee = n.getInetAddresses();
        while (ee.hasMoreElements()) {
          InetAddress i = (InetAddress) ee.nextElement();
          if (!i.getHostAddress().contains("127.0.0.1") && !i.getHostAddress().contains(":") && !i.getHostAddress().contains("%")) {
            ip = i.getHostAddress();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return ip;
  }

}
