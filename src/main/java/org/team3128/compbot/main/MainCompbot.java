package org.team3128.compbot.main;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;

import org.team3128.common.generics.RobotConstants;

import org.team3128.common.NarwhalRobot;
import org.team3128.common.control.trajectory.Trajectory;
import org.team3128.common.control.trajectory.TrajectoryGenerator;
import org.team3128.common.control.trajectory.constraint.TrajectoryConstraint;
import org.team3128.common.drive.DriveCommandRunning;
import org.team3128.common.hardware.limelight.Limelight;
import org.team3128.common.hardware.gyroscope.Gyro;
import org.team3128.common.hardware.gyroscope.NavX;
import org.team3128.common.utility.units.Angle;
import org.team3128.common.utility.units.Length;
import org.team3128.common.vision.CmdHorizontalOffsetFeedbackDrive;
import org.team3128.athos.subsystems.Constants;
import org.team3128.common.utility.Log;
import org.team3128.common.utility.RobotMath;
import org.team3128.common.utility.datatypes.PIDConstants;
import org.team3128.common.narwhaldashboard.NarwhalDashboard;
import org.team3128.common.listener.ListenerManager;
import org.team3128.common.listener.controllers.ControllerExtreme3D;
import org.team3128.common.listener.controltypes.Button;
import org.team3128.common.hardware.motor.LazyCANSparkMax;
import org.team3128.common.utility.math.Pose2D;
import org.team3128.common.utility.math.Rotation2D;
import org.team3128.common.utility.test_suite.CanDevices;
import org.team3128.common.utility.test_suite.ErrorCatcherUtility;
import org.team3128.compbot.subsystems.FalconDrive;
import org.team3128.compbot.subsystems.RobotTracker;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.util.ArrayList;
import java.util.concurrent.*;

import org.team3128.common.generics.ThreadScheduler;

public class MainCompbot extends NarwhalRobot {

    public DigitalInput digitalInput;
    public DigitalInput digitalInput2;

    FalconDrive drive = FalconDrive.getInstance();
    RobotTracker robotTracker = RobotTracker.getInstance();

    ExecutorService executor = Executors.newFixedThreadPool(4);
    ThreadScheduler scheduler = new ThreadScheduler();
    Thread auto;

    public Joystick joystick;
    public ListenerManager lm;
    public Gyro gyro;
    public PowerDistributionPanel pdp;

    public NetworkTable table;
    public NetworkTable limelightTable;

    public double kP = Constants.K_AUTO_LEFT_P;
    public double kD = Constants.K_AUTO_LEFT_D;
    public double kF = Constants.K_AUTO_LEFT_F;

    public double startTime = 0;

    public String trackerCSV = "Time, X, Y, Theta, Xdes, Ydes";

    public ArrayList<Pose2D> waypoints = new ArrayList<Pose2D>();
    public Trajectory trajectory;

    public boolean inPlace = false;
    public boolean inPlace2 = false;

    public int countBalls = 0;
    public int countBalls2 = 0;

    Limelight limelight = new Limelight("limelight-c", 26.0, 0, 0, 30);
    Limelight[] limelights = new Limelight[1];
    public ErrorCatcherUtility errorCatcher;
    public static CanDevices[] CanChain = new CanDevices[42];
    public static void setCanChain(){
        CanChain[0] = Constants.rightDriveLeader;
        CanChain[1] = Constants.rightDriveFollower;
        CanChain[2] = Constants.leftDriveFollower;
        CanChain[3] = Constants.leftDriveLeader;
        CanChain[4] = Constants.PDP;
    }

    @Override
    protected void constructHardware() {

        digitalInput = new DigitalInput(0);
        digitalInput2 = new DigitalInput(1);
        
        scheduler.schedule(drive, executor);
        scheduler.schedule(robotTracker, executor);

        // // Instatiator if we're using the NavX
        // gyro = new NavX();

        // // Instatiator if we're using the KoP Gyro
        // gyro = new AnalogDevicesGyro();
        // //gyro.recalibrate();

        joystick = new Joystick(1);
        lm = new ListenerManager(joystick);
        addListenerManager(lm);

        // display PID coefficients on SmartDashboard
        SmartDashboard.putNumber("P Gain", kP);
        SmartDashboard.putNumber("D Gain", kD);
        SmartDashboard.putNumber("F Gain", kF);

        // initialization of auto test suite
        
        limelights[0] = limelight;
        pdp = new PowerDistributionPanel(0);
        Constants.rightDriveLeader = new CanDevices(CanDevices.DeviceType.FALCON, 0, "Right Drive Leader", null , null, null, drive.rightTalon, null);
        Constants.rightDriveFollower = new CanDevices(CanDevices.DeviceType.FALCON, 1, "Right Drive Follower", null, null , null, drive.rightTalonSlave, null);
        Constants.leftDriveLeader = new CanDevices(CanDevices.DeviceType.FALCON, 2, "Left Drive Leader", null , null, null, drive.leftTalon, null);
        Constants.leftDriveFollower = new CanDevices(CanDevices.DeviceType.FALCON, 3, "Left Drive Follower", null, null , null, drive.leftTalonSlave, null);
        Constants.PDP = new CanDevices(CanDevices.DeviceType.PDP, 0, "Power Distribution Panel", null, null, null, null, pdp);
        setCanChain();
        errorCatcher = new ErrorCatcherUtility(CanChain,limelights,drive);

        NarwhalDashboard.addButton("ErrorCatcher", (boolean down) -> {
            if (down) {
                //Janky fix
                
               errorCatcher.testEverything();
               
               errorCatcher.testEverything();
            }
        });
    }

    @Override
    protected void constructAutoPrograms() {
    }

    @Override
    protected void setupListeners() {
        lm.nameControl(ControllerExtreme3D.TWIST, "MoveTurn");
        lm.nameControl(ControllerExtreme3D.JOYY, "MoveForwards");
        lm.nameControl(ControllerExtreme3D.THROTTLE, "Throttle");
        lm.nameControl(new Button(5), "ResetGyro");
        lm.nameControl(new Button(6), "PrintCSV");
        lm.nameControl(new Button(3), "ClearTracker");
        lm.nameControl(new Button(4), "ClearCSV");

        lm.addMultiListener(() -> {
            drive.arcadeDrive(-0.7 * RobotMath.thresh(lm.getAxis("MoveTurn"), 0.1),
                    -1.0 * RobotMath.thresh(lm.getAxis("MoveForwards"), 0.1), -1.0 * lm.getAxis("Throttle"), true);

        }, "MoveTurn", "MoveForwards", "Throttle");

        lm.nameControl(ControllerExtreme3D.TRIGGER, "AlignToTarget");
        lm.addButtonDownListener("AlignToTarget", () -> {
            // TODO: Add current implementation of vision alignment
            Log.info("MainCompbot.java", "[Vision Alignment] Not created yet, would've started");
        });
        lm.addButtonUpListener("AlignToTarget", () -> {
            Log.info("MainCompbot.java", "[Vision Alignment] Not created yet, would've ended");
        });

        lm.addButtonDownListener("ResetGyro", () -> {
            drive.resetGyro();
        });
        lm.addButtonDownListener("PrintCSV", () -> {
            Log.info("MainCompbot", trackerCSV);
        });
        lm.addButtonDownListener("ClearCSV", () -> {
            trackerCSV = "Time, X, Y, Theta, Xdes, Ydes";
            Log.info("MainCompbot", "CSV CLEARED");
            startTime = Timer.getFPGATimestamp();
        });

        lm.addButtonDownListener("ClearTracker", () -> {
            robotTracker.resetOdometry();
        });

    }

    @Override
    protected void teleopPeriodic() {
        
        // logic for photoelectric sensor 
        if (inPlace == false && digitalInput.get()){
            countBalls++;
            System.out.println("Number of balls: " + countBalls);
            inPlace = true;
        }
        else if (!digitalInput.get()) {
            inPlace = false;
        }
        
        if (inPlace2 == false && digitalInput2.get()){
            countBalls--;
            System.out.println("Number of balls: " + countBalls);
            inPlace2 = true;
        }
        else if (!digitalInput2.get()) {
            inPlace2 = false;
        }

    }

    double maxLeftSpeed = 0;
    double maxRightSpeed = 0;
    double maxSpeed = 0;
    double minLeftSpeed = 0;
    double minRightSpeed = 0;
    double minSpeed = 0;

    double currentLeftSpeed;
    double currentLeftDistance;
    double currentRightSpeed;
    double currentRightDistance;
    double currentSpeed;
    double currentDistance;

    @Override
    protected void updateDashboard() {
        currentLeftSpeed = drive.getLeftSpeed();
        currentLeftDistance = drive.getLeftDistance();
        currentRightSpeed = drive.getRightSpeed();
        currentRightDistance = drive.getRightDistance();

        currentSpeed = drive.getSpeed();
        currentDistance = drive.getDistance();

        NarwhalDashboard.put("time", DriverStation.getInstance().getMatchTime());
        NarwhalDashboard.put("voltage", RobotController.getBatteryVoltage());

        SmartDashboard.putNumber("Gyro Angle", drive.getAngle());
        SmartDashboard.putNumber("Left Distance", currentLeftDistance);
        SmartDashboard.putNumber("Right Distance", currentRightDistance);

        SmartDashboard.putNumber("Distance", currentDistance);

        SmartDashboard.putNumber("Left Velocity", currentLeftSpeed);
        SmartDashboard.putNumber("Right Velocity", currentRightSpeed);

        SmartDashboard.putNumber("Velocity", drive.getSpeed());

        SmartDashboard.putNumber("RobotTracker - x:", robotTracker.getOdometry().getTranslation().getX());
        SmartDashboard.putNumber("RobotTracker - y:", robotTracker.getOdometry().getTranslation().getY());
        SmartDashboard.putNumber("RobotTracker - theta:", robotTracker.getOdometry().getRotation().getDegrees());

        maxLeftSpeed = Math.max(maxLeftSpeed, currentLeftSpeed);
        maxRightSpeed = Math.max(maxRightSpeed, currentRightSpeed);
        maxSpeed = Math.max(maxSpeed, currentSpeed);
        minLeftSpeed = Math.min(minLeftSpeed, currentLeftSpeed);
        minRightSpeed = Math.min(minRightSpeed, currentLeftSpeed);
        minSpeed = Math.min(minSpeed, currentSpeed);

        SmartDashboard.putNumber("Max Left Speed", maxLeftSpeed);
        SmartDashboard.putNumber("Min Left Speed", minLeftSpeed);
        SmartDashboard.putNumber("Max Right Speed", maxRightSpeed);
        SmartDashboard.putNumber("Min Right Speed", minRightSpeed);

        SmartDashboard.putNumber("Max Speed", maxSpeed);
        SmartDashboard.putNumber("Min Speed", minSpeed);

        // read PID coefficients from SmartDashboard
        double p = SmartDashboard.getNumber("P Gain", 0);
        double d = SmartDashboard.getNumber("D Gain", 0);
        double f = SmartDashboard.getNumber("F Gain", 0);

        boolean hasChanged = false;
        if ((p != kP)) {
            kP = p;
            hasChanged = true;
        }
        if ((d != kD)) {
            kD = d;
            hasChanged = true;
        }
        if ((f != kF)) {
            kF = f;
            hasChanged = true;
        }
        if (hasChanged) {
            drive.setDualVelocityPID(kP, kD, kF);
        }

        trackerCSV += "\n" + String.valueOf(Timer.getFPGATimestamp() - startTime) + ","
                + String.valueOf(robotTracker.getOdometry().translationMat.getX()) + ","
                + String.valueOf(robotTracker.getOdometry().translationMat.getY()) + ","
                + String.valueOf(robotTracker.getOdometry().rotationMat.getDegrees()) + ","
                + String.valueOf(robotTracker.trajOdometry.translationMat.getX()) + ","
                + String.valueOf(robotTracker.trajOdometry.translationMat.getY());
    }

    @Override
    protected void teleopInit() {
        scheduler.resume();
    }

    @Override
    protected void autonomousInit() {
        waypoints.add(new Pose2D(0, 0, Rotation2D.fromDegrees(0)));
        waypoints.add(
                new Pose2D(0 * Constants.inchesToMeters, 70 * Constants.inchesToMeters, Rotation2D.fromDegrees(-45)));

        trajectory = TrajectoryGenerator.generateTrajectory(waypoints, new ArrayList<TrajectoryConstraint>(), 0, 0,
                120 * Constants.inchesToMeters, 0.5, false);

        trackerCSV = "Time, X, Y, Theta, Xdes, Ydes";
        Log.info("MainCompbot", "going into autonomousinit");
        scheduler.resume();
        robotTracker.resetOdometry();
        drive.setAutoTrajectory(trajectory, false);
        drive.startTrajectory();
        startTime = Timer.getFPGATimestamp();
    }

    @Override
    protected void disabledInit() {
        scheduler.pause();
    }

    public static void main(String... args) {
        RobotBase.startRobot(MainCompbot::new);
    }

    @Override
    public void endCompetition() {
        // TODO Auto-generated method stub

    }
}