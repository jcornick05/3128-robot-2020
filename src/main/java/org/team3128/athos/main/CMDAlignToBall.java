package org.team3128.athos.main;

import org.team3128.common.hardware.limelight.Limelight;
import org.team3128.common.drive.DriveCommandRunning;
import org.team3128.common.vision.CmdHorizontalOffsetFeedbackDrive;

import edu.wpi.first.wpilibj.command.CommandGroup;
import edu.wpi.first.wpilibj.interfaces.Gyro;

import org.team3128.common.utility.datatypes.PIDConstants;
import org.team3128.common.utility.units.Angle;
import org.team3128.common.utility.units.Length;


public class CMDAlignToBall extends CommandGroup {
    
    DriveCommandRunning cmdRunning;
    double goalHorizontalOffset=0.8;
    double decelerationStartDistance=24;
    double decelerationEndDistance;
    double blindThreshold;
    public CMDAlignToBall(org.team3128.common.hardware.gyroscope.Gyro gyro, Limelight bottomLimelight, DriveCommandRunning cmdRunning, PIDConstants visionPID, PIDConstants blindPID) {
        double targetHeight=3.5*Length.in;
        
        Limelight distLimelight = bottomLimelight;
        Limelight txLimelight = bottomLimelight;

		addSequential(new CmdHorizontalOffsetFeedbackDrive(gyro, txLimelight, distLimelight, cmdRunning, targetHeight,
        visionPID, -2 * Angle.DEGREES, 2.5 * Length.ft, 0.6666666666666666666666 * Length.ft,
        blindPID, 20 * Angle.DEGREES));
        
        
    }
}