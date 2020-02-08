package org.team3128.compbot.subsystems;

import org.team3128.common.utility.Log;

import edu.wpi.first.wpilibj.DigitalInput;

import org.team3128.common.generics.Threaded;
import org.team3128.common.hardware.motor.LazyCANSparkMax;

import com.revrobotics.CANEncoder;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import org.team3128.common.game_elements.Ball;

public class Hopper extends Threaded {

    LazyCANSparkMax ALIGN_MOTOR, HOPPER_FEEDER_MOTOR, CORNER_MOTOR, SHOOTER_FEEDER_MOTOR;
    CANEncoder ALIGN_ENCODER, HOPPER_FEEDER_ENCODER, CORNER_ENCODER, SHOOTER_FEEDER_ENCODER;
    DigitalInput SENSOR_1, SENSOR_2, SENSOR_3, SENSOR_4;

    Ball[] ballArray;

    public static final Hopper instance = new Hopper();

    public Hopper getInstance() {
        return instance;
    }

    private Hopper() {
        configMotors();
        configEncoders();
        configSensors();
    }

    @Override
    public void update() {

    }

    public void configMotors() {
        ALIGN_MOTOR = new LazyCANSparkMax(Constants.ALIGN_MOTOR_ID, MotorType.kBrushless);
        HOPPER_FEEDER_MOTOR = new LazyCANSparkMax(Constants.HOPPER_FEEDER_MOTOR_ID, MotorType.kBrushless);
        CORNER_MOTOR = new LazyCANSparkMax(Constants.CORNER_MOTOR_ID, MotorType.kBrushless);
        SHOOTER_FEEDER_MOTOR = new LazyCANSparkMax(Constants.SHOOTER_FEEDER_MOTOR_ID, MotorType.kBrushless);
    }

    public void configEncoders() {
        ALIGN_ENCODER = ALIGN_MOTOR.getEncoder();
        HOPPER_FEEDER_ENCODER = HOPPER_FEEDER_MOTOR.getEncoder();
        CORNER_ENCODER = CORNER_MOTOR.getEncoder();
        SHOOTER_FEEDER_ENCODER = SHOOTER_FEEDER_MOTOR.getEncoder();
    }

    public void configSensors() {
        SENSOR_1 = new DigitalInput(Constants.SENSOR_1_ID);
        SENSOR_2 = new DigitalInput(Constants.SENSOR_2_ID);
        SENSOR_3 = new DigitalInput(Constants.SENSOR_3_ID);
        SENSOR_4 = new DigitalInput(Constants.SENSOR_4_ID);
    }

}