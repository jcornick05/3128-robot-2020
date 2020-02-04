//formerly wallace ;)

package org.team3128.aramis.main;


import org.team3128.common.utility.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;

import org.team3128.common.hardware.motor.*;

import org.team3128.common.NarwhalRobot;
import org.team3128.common.drive.DriveCommandRunning;
import org.team3128.common.drive.SRXTankDrive;
import org.team3128.common.drive.SRXTankDrive.Wheelbase;
import org.team3128.common.drive.calibrationutility.DriveCalibrationUtility;
import org.team3128.common.hardware.limelight.Limelight;
import org.team3128.common.vision.CmdHorizontalOffsetFeedbackDrive;
import org.team3128.aramis.subsystems.Constants;
import org.team3128.common.utility.Log;
import org.team3128.common.utility.RobotMath;
import org.team3128.common.utility.datatypes.PIDConstants;
import org.team3128.common.utility.units.Angle;
import org.team3128.common.utility.units.Length;
import org.team3128.common.narwhaldashboard.NarwhalDashboard;
import org.team3128.common.listener.ListenerManager;
import org.team3128.common.listener.controllers.ControllerExtreme3D;
import org.team3128.common.listener.controltypes.Button;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.team3128.common.utility.test_suite.*;

public class MainAramis extends NarwhalRobot {
    public LazyTalonSRX rightDriveLeader;
    public VictorSPX rightDriveFollower;
    public LazyTalonSRX leftDriveLeader;
    public VictorSPX leftDriveFollower;
    public PowerDistributionPanel pdp;

    public SRXTankDrive tankDrive;

    public Joystick joystick;
    public ListenerManager lm;

    public PIDConstants leftMotionProfilePID, rightMotionProfilePID;

    public double speedScalar;

    public double maxLeftSpeed = 0;
    public double maxRightSpeed = 0;
    public double leftSpeed = 0;
    public double rightSpeed = 0;
    public NetworkTable table;
    public NetworkTable table2;

    PIDConstants visionPID, blindPID;

    public NetworkTable limelightTable;

    public DriveCalibrationUtility dcu;
    public Wheelbase calculatedWheelbase;

    public CmdHorizontalOffsetFeedbackDrive alignCommand;
    private DriveCommandRunning driveCmdRunning;

    public Limelight limelight = new Limelight("limelight", 26 * Angle.DEGREES, 6.15 * Length.in, 0 * Length.in,
            14.5 * Length.in);

    BufferedWriter bw = null;
    FileWriter fw = null;
    InputStream is = null;
    OutputStream os = null;
    File file;
    File usbFile;
    String csvString = "";


    public ErrorCatcherUtility errorCatcher;
    public static CanDevices[] CanChain = new CanDevices[42];
    public static void setCanChain(){
        CanChain[0] = Constants.leftDriveLeader;
        CanChain[1] = Constants.leftDriveFollower;
        CanChain[2] = Constants.rightDriveLeader;
        CanChain[3] = Constants.rightDriveFollower;
        CanChain[4] = Constants.PDP;
    }


    @Override
    protected void constructHardware() {
        try {
            // file = new File("C:/log.txt"); //ooohhh
            usbFile = new File("/media/sda1/limelight-loggerification-test-1.txt"); // i wuz in a hurry so no comments
                                                                                    // sorry y'all'ses

            // if(file.exists()) {
            // file.delete();
            // file.createNewFile();
            // } else {
            // file.createNewFile();
            // }

            if (usbFile.exists()) {
                usbFile.delete();
                usbFile.createNewFile();
            } else {
                usbFile.createNewFile();
            }

            fw = new FileWriter(usbFile);
            bw = new BufferedWriter(fw);
            bw.write("FPGA Time, tx, ty, ts, ta, thor, tvert, tshort, tlong, tv, x, y, z, pitch, yaw, roll");
            Log.info("im here", "im here");
            // bw.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        

        limelightTable = NetworkTableInstance.getDefault().getTable("limelight");

        rightDriveLeader = new LazyTalonSRX(15);
        rightDriveFollower = new VictorSPX(6);

        leftDriveLeader = new LazyTalonSRX(13);
        leftDriveFollower = new VictorSPX(5);

        pdp = new PowerDistributionPanel(0);
        
        rightDriveLeader.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0,
                Constants.CAN_TIMEOUT);
        rightDriveFollower.set(ControlMode.Follower, rightDriveLeader.getDeviceID());

        leftDriveLeader.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, Constants.CAN_TIMEOUT);
        leftDriveFollower.set(ControlMode.Follower, leftDriveLeader.getDeviceID());

        double wheelCirc = 13.21 * Length.in;
        // double wheelBase = 31.5 * Length.in; 5 feet
        // double wheelBase = 32.25 * Length.in; 4 feet
        double wheelBase = 32.3 * Length.in;
        int robotFreeSpeed = 3700;

        SRXTankDrive.initialize(leftDriveLeader, rightDriveLeader, wheelCirc, wheelBase, robotFreeSpeed);

        leftDriveLeader.setInverted(false);
        leftDriveFollower.setInverted(false);

        rightDriveLeader.setInverted(true);
        rightDriveFollower.setInverted(true);

        leftDriveLeader.setSensorPhase(true);
        rightDriveLeader.setSensorPhase(true);

        tankDrive = SRXTankDrive.getInstance();
        tankDrive.setLeftSpeedScalar(1.0);
        tankDrive.setRightSpeedScalar(.983);

        // Instatiator if we're using the NavX
        // gyro = new NavX();

        // Instatiator if we're using the KoP Gyro
        // gyro = new AnalogDevicesGyro();
        // ((AnalogDevicesGyro) gyro).recalibrate();

        joystick = new Joystick(1);
        lm = new ListenerManager(joystick);
        addListenerManager(lm);

        // Vision
        visionPID = new PIDConstants(0, 0.02, 0.0, 0.00001);
        blindPID = new PIDConstants(0.1, 0, 0, 0);
        driveCmdRunning = new DriveCommandRunning();

        //Error Catcher (Auto Test Suite)
        Constants.leftDriveLeader = new CanDevices(CanDevices.DeviceType.TALON, 13, "Left Drive Leader", leftDriveLeader, null, null, null, null);
        Constants.leftDriveFollower = new CanDevices(CanDevices.DeviceType.VICTOR, 5, "Left Drive Follower", null, leftDriveFollower, null, null, null);
        Constants.rightDriveLeader = new CanDevices(CanDevices.DeviceType.TALON, 15, "Right Drive Leader", rightDriveLeader, null, null, null, null);
        Constants.rightDriveFollower = new CanDevices(CanDevices.DeviceType.VICTOR, 6, "Right Drive Follower", null, rightDriveFollower, null, null, null);
        Constants.PDP = new CanDevices(CanDevices.DeviceType.PDP, 0, "Power Distribution Panel", null, null, null, null, pdp);
        //errorCatcher = new ErrorCatcherUtility(CanChain);
        setCanChain();

        // DCU
        // DriveCalibrationUtility.initialize(gyro, visionPID);
        //dcu = DriveCalibrationUtility.getInstance();

        //dcu.initNarwhalDashboard();
        NarwhalDashboard.addButton("ErrorCatcher", (boolean down) -> {
            if (down) {
                //errorCatcher.ErrorCatcher();
            }
        });
    }

    @Override
    protected void constructAutoPrograms() {
        // NarwhalDashboard.addAuto("Turn", new CmdInPlaceTurnTest());

        // NarwhalDashboard.addAuto("Arc, 5ft, Left", tankDrive.new CmdArcTurn(5 *
        // Length.ft, 90, Direction.LEFT, .75, 10000));
        // NarwhalDashboard.addAuto("Arc, 4ft, Left", tankDrive.new CmdArcTurn(4 *
        // Length.ft, 90, Direction.LEFT, .75, 10000));
        // NarwhalDashboard.addAuto("Arc, 3ft, Left", tankDrive.new CmdArcTurn(3 *
        // Length.ft, 90, Direction.LEFT, .75, 10000));

        // NarwhalDashboard.addAuto("Arc, 5ft, Right", tankDrive.new CmdArcTurn(5 *
        // Length.ft, 90, Direction.RIGHT, .75, 10000));
        // NarwhalDashboard.addAuto("Arc, 4ft, Right", tankDrive.new CmdArcTurn(5 *
        // Length.ft, 90, Direction.RIGHT, .75, 10000));
        // NarwhalDashboard.addAuto("Arc, 3ft, Right", tankDrive.new CmdArcTurn(5 *
        // Length.ft, 90, Direction.RIGHT, .75, 10000));

        // NarwhalDashboard.addAuto("In-Place, 180, Left", tankDrive.new
        // CmdInPlaceTurn(180 * Angle.DEGREES, Direction.LEFT, 0.75, 5000));

        // NarwhalDashboard.addAuto("Please Work", new CmdPleaseWorkTurnTest(gyro));

        // NarwhalDashboard.addAuto("Forward", new CmdDriveForward());
        // // NarwhalDashboard.addAuto("Test", new Test(tankDrive, ahrs));
        // // NarwhalDashboard.addAuto("Wheel Base Test", new
        // CmdCalibrateWheelbase(ahrs,
        // // 10, 1000, 1500, calculatedWheelbase));
        // // NarwhalDashboard.addAuto("Forward CV", new CmdDriveForwardCVTest());
        // NarwhalDashboard.addAuto("Routemaker Test", new CmdRoutemakerTest());
        // // NarwhalDashboard.addAuto("Heading Then Arc Turn", new
        // // CmdHeadingThenArc(limelight));
        // NarwhalDashboard.addAuto("Fancy Wheel Base Calibration", new
        // CmdFancyCalibrateWheelBase(gyro));
        // NarwhalDashboard.addAuto("CmdDynamicAdjsut", new CmdDynamicAdjustTest(gyro,
        // limelight));
        // previous speeds that were used were 2000, 4000 (arbitrarily picked)
    }

    @Override
    protected void setupListeners() {
        lm.nameControl(ControllerExtreme3D.TWIST, "MoveTurn");
        lm.nameControl(ControllerExtreme3D.JOYY, "MoveForwards");
        lm.nameControl(ControllerExtreme3D.THROTTLE, "Throttle");

        lm.addMultiListener(() -> {
            if (!driveCmdRunning.isRunning) {
                tankDrive.arcadeDrive(-0.7 * RobotMath.thresh(lm.getAxis("MoveTurn"), 0.1),
                        -1.0 * RobotMath.thresh(lm.getAxis("MoveForwards"), 0.1), -1.0 * lm.getAxis("Throttle"), true);
            }

        }, "MoveTurn", "MoveForwards", "Throttle");

        lm.nameControl(new Button(12), "FullSpeed");
        lm.addButtonDownListener("FullSpeed", () -> {
            tankDrive.tankDrive(-1, -1);
        });
        lm.addButtonUpListener("FullSpeed", () -> {
            tankDrive.tankDrive(0, 0);
        });

        lm.nameControl(new Button(2), "LightOn");
        lm.addButtonDownListener("LightOn", () -> {
            limelightTable.getEntry("ledMode").setNumber(3);
            Log.debug("Limelight Latency", String.valueOf(limelightTable.getEntry("tl").getDouble(0.0)));

        });

        lm.nameControl(ControllerExtreme3D.TRIGGER, "AlignToTarget");
        lm.addButtonDownListener("AlignToTarget", () -> {
            // alignCommand = new CmdHorizontalOffsetFeedbackDrive(gyro, limelight,
            // visionPID, driveCmdRunning,
            // -1 * Angle.DEGREES, 14.5 * Length.in,
            // DeepSpaceConstants.DECELERATE_START_DISTANCE,
            // DeepSpaceConstants.DECELERATE_END_DISTANCE,
            // blindPID, false);
            // alignCommand.start();
        });
        lm.addButtonUpListener("AlignToTarget", () -> {
            alignCommand.cancel();
            alignCommand = null;
        });

        lm.nameControl(new Button(3), "startGetValues");
        lm.addButtonDownListener("startGetValues", () -> {
            // try {

            // fw = new FileWriter(file);
            // bw = new BufferedWriter(fw);
            // bw.write(limelight.getValues(30).toString());
            // bw.close();

            // } catch (IOException ioe) {
            // ioe.printStackTrace();
            // }
            Log.info("MainAthos", "starting getting data");
            SmartDashboard.putBoolean("gettingData", true);
            // csvString += (Long.toString(RobotController.GetFPGATime())
            // + limelight.getValues(30).toString() + "\n");
        });

        lm.nameControl(new Button(4), "stopGetValues");
        lm.addButtonDownListener("stopGetValues", () -> {
            Log.info("MainAthos", "stopping getting data");
            SmartDashboard.putBoolean("gettingData", false);
        });

        lm.nameControl(new Button(5), "writeValues");
        lm.addButtonDownListener("writeValues", () -> {
            try {
                Log.info("MainAthos", "writing values");
                bw.write(csvString);
                // is = new FileInputStream(file);
                // os = new FileOutputStream(usbFile);

                // is.transferTo(os);
                bw.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            csvString = "";
        });

        lm.nameControl(new Button(6), "closeWriter");
        lm.addButtonDownListener("closeWriter", () -> {
            Log.info("MainAramis", "closing buffered writer");
            try {
                bw.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
    }

    @Override
    protected void teleopPeriodic() {
    }

    @Override
    protected void updateDashboard() {
        // SmartDashboard.putNumber("Gyro Angle", gyro.getAngle());
        // SmartDashboard.putNumber("getRate", gyro.getRate());

        maxLeftSpeed = Math.max(leftDriveLeader.getSelectedSensorVelocity(), maxLeftSpeed);
        maxRightSpeed = Math.max(rightDriveLeader.getSelectedSensorVelocity(), maxRightSpeed);

        SmartDashboard.putNumber("Max Left Speed", maxLeftSpeed);
        SmartDashboard.putNumber("Max Right Speed", maxRightSpeed);

        SmartDashboard.putNumber("Left Speed", leftDriveLeader.getSelectedSensorVelocity());
        SmartDashboard.putNumber("Right Speed", rightDriveLeader.getSelectedSensorVelocity());

        SmartDashboard.putNumber("Left Position", leftDriveLeader.getSelectedSensorPosition());
        SmartDashboard.putNumber("Right Position", rightDriveLeader.getSelectedSensorPosition());

        NarwhalDashboard.put("time", DriverStation.getInstance().getMatchTime());
        NarwhalDashboard.put("voltage", RobotController.getBatteryVoltage());

        //dcu.tickNarwhalDashboard();

        if (SmartDashboard.getBoolean("gettingData", false)) {
            csvString += (Long.toString(RobotController.getFPGATime()) + limelight.getValues(5).toString() + "\n");
        }

        // USB.write(limelight.getValues(30).toString());
        /*
         * try {
         * 
         * bw = new BufferedWriter(fw); bw.write(limelight.getValues(30).toString());
         * bw.close();
         * 
         * is = new FileInputStream(file); os = new FileOutputStream(usbFile);
         * 
         * is.transferTo(os);
         * 
         * } catch (IOException ioe) { ioe.printStackTrace(); }
         */

    }

    public static void main(String... args) {
        RobotBase.startRobot(MainAramis::new);
    }

    @Override
    public void endCompetition() {
        // TODO Auto-generated method stub

    }
}