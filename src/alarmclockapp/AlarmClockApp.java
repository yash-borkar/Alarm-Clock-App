package alarmclockapp;

import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;

public class AlarmClockApp {
    private JFrame frame;
    private JSpinner timeSpinner;
    private JTextField noteField;
    private DefaultListModel<String> alarmsListModel;
    private JList<String> alarmsList;
    private Map<String, LocalTime> activeAlarms;
    private java.util.Timer timer;
    private Clip alarmSound;
    private JLabel selectedFileLabel;

    public AlarmClockApp() {
        activeAlarms = new HashMap<>();
        setupSound();
        initializeGUI();
        startAlarmChecker();
    }

    private void setupSound() {
        try {
            File soundFile = new File("C:\\Users\\imyas\\OneDrive\\Documents\\NetBeansProjects\\AlarmClockApp\\src\\alarmclockapp\\beep.wav");
            if (!soundFile.exists()) {
                System.err.println("Sound file not found!");
                return;
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            alarmSound = AudioSystem.getClip();
            alarmSound.open(audioStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeGUI() {
    frame = new JFrame("Alarm Clock");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(500, 450);
    frame.setLayout(new BorderLayout(10, 10));

    JPanel inputPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    SpinnerDateModel timeModel = new SpinnerDateModel();
    timeSpinner = new JSpinner(timeModel);
    JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "hh:mm a");
    timeSpinner.setEditor(timeEditor);
    timeSpinner.setValue(new Date());

    noteField = new JTextField(15);
    JButton addAlarmButton = new JButton("Add Alarm");
    addAlarmButton.addActionListener(e -> addAlarm());

    JButton selectSoundButton = new JButton("Select Alarm Sound");
    selectSoundButton.addActionListener(e -> selectAlarmSound());

    selectedFileLabel = new JLabel("No file selected."); // Initialize the label

    gbc.gridx = 0;
    gbc.gridy = 0;
    inputPanel.add(new JLabel("Set Time:"), gbc);
    gbc.gridx = 1;
    inputPanel.add(timeSpinner, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    inputPanel.add(new JLabel("Note:"), gbc);
    gbc.gridx = 1;
    inputPanel.add(noteField, gbc);

    gbc.gridx = 1;
    gbc.gridy = 2;
    inputPanel.add(addAlarmButton, gbc);

    gbc.gridx = 1;
    gbc.gridy = 3;
    inputPanel.add(selectSoundButton, gbc);

    gbc.gridx = 1;
    gbc.gridy = 4;
    inputPanel.add(selectedFileLabel, gbc); 

    alarmsListModel = new DefaultListModel<>();
    alarmsList = new JList<>(alarmsListModel);
    JScrollPane scrollPane = new JScrollPane(alarmsList);

    JButton deleteButton = new JButton("Delete Selected");
    deleteButton.addActionListener(e -> deleteSelectedAlarm());

    JPanel listPanel = new JPanel(new BorderLayout());
    listPanel.add(new JLabel("Active Alarms:"), BorderLayout.NORTH);
    listPanel.add(scrollPane, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(deleteButton);
    listPanel.add(buttonPanel, BorderLayout.SOUTH);

    frame.add(inputPanel, BorderLayout.NORTH);
    frame.add(listPanel, BorderLayout.CENTER);
    frame.setVisible(true);
}

private void selectAlarmSound() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Select Alarm Sound");
    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("WAV Files", "wav"));

    int result = fileChooser.showOpenDialog(frame);
    if (result == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile.exists()) {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(selectedFile);
                alarmSound = AudioSystem.getClip();
                alarmSound.open(audioStream);
                selectedFileLabel.setText("Selected: " + selectedFile.getName());
                JOptionPane.showMessageDialog(frame, "Alarm sound set successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                selectedFileLabel.setText("Error loading file."); 
                JOptionPane.showMessageDialog(frame, "Error loading sound file.", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        } else {
            selectedFileLabel.setText("File does not exist."); 
            JOptionPane.showMessageDialog(frame, "Selected file does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

    private void addAlarm() {
    Date selectedDate = (Date) timeSpinner.getValue();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(selectedDate);
    
    int hour = calendar.get(Calendar.HOUR); 
    int minute = calendar.get(Calendar.MINUTE);
    int ampm = calendar.get(Calendar.AM_PM); 
    
    if (ampm == Calendar.PM && hour != 12) {
        hour += 12; 
    } else if (ampm == Calendar.AM && hour == 12) {
        hour = 0;
    }
    
    LocalTime alarmTime = LocalTime.of(hour, minute);

    boolean alarmExists = activeAlarms.values().stream()
            .anyMatch(existingTime -> existingTime.getHour() == alarmTime.getHour() && existingTime.getMinute() == alarmTime.getMinute());

    if (alarmExists) {
        JOptionPane.showMessageDialog(frame, "An alarm for this time already exists.", "Duplicate Alarm", JOptionPane.WARNING_MESSAGE);
        return;
    }

    String note = noteField.getText().trim();
    String alarmString = String.format("%s - %s",
            alarmTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
            note.isEmpty() ? "No note" : note);

    alarmsListModel.addElement(alarmString);
    activeAlarms.put(alarmString, alarmTime);
    noteField.setText("");
}


    private void deleteSelectedAlarm() {
        int selectedIndex = alarmsList.getSelectedIndex();
        if (selectedIndex != -1) {
            String alarmString = alarmsListModel.get(selectedIndex);
            activeAlarms.remove(alarmString);
            alarmsListModel.remove(selectedIndex);
        }
    }

    private void stopAllAlarms() {
        activeAlarms.clear();
        alarmsListModel.clear();
    }

    private void startAlarmChecker() {
        timer = new java.util.Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAlarms();
            }
        }, 0, 1000);
    }

    private void checkAlarms() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        Iterator<Map.Entry<String, LocalTime>> iterator = activeAlarms.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, LocalTime> entry = iterator.next();
            if (now.equals(entry.getValue())) {
                playAlarm(entry.getKey());
                iterator.remove();
                SwingUtilities.invokeLater(() -> alarmsListModel.removeElement(entry.getKey()));
            }
        }
    }

    private void playAlarm(String alarmString) {
        if (alarmSound != null) {
            new Thread(() -> {
                alarmSound.setFramePosition(0);
                alarmSound.start();
            }).start();
        }

        SwingUtilities.invokeLater(() -> {
            JDialog alarmDialog = new JDialog(frame, "Alarm", true);
            alarmDialog.setSize(300, 200);
            alarmDialog.setLayout(new GridLayout(3, 1, 5, 5));

            JLabel alarmMessage = new JLabel(alarmString, SwingConstants.CENTER);

            JButton snoozeButton = new JButton("Snooze (5 min)");
            snoozeButton.addActionListener(e -> {
                snoozeAlarm(alarmString);
                alarmDialog.dispose();
                if (alarmSound != null) {
                    alarmSound.stop();
                }
            });

            JButton stopButton = new JButton("Stop");
            stopButton.addActionListener(e -> {
                alarmDialog.dispose();
                if (alarmSound != null) {
                    alarmSound.stop();
                }
            });

            alarmDialog.add(alarmMessage);
            alarmDialog.add(snoozeButton);
            alarmDialog.add(stopButton);

            alarmDialog.setLocationRelativeTo(frame);
            alarmDialog.setVisible(true);
        });
    }

    private void snoozeAlarm(String alarmString) {
        LocalTime newTime = LocalTime.now().plusMinutes(5).withSecond(0).withNano(0);
        String newAlarmString = String.format("%s - %s (Snoozed)",
                newTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                alarmString.split(" - ")[1]);

        activeAlarms.put(newAlarmString, newTime);
        alarmsListModel.addElement(newAlarmString);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AlarmClockApp::new);
    }
}