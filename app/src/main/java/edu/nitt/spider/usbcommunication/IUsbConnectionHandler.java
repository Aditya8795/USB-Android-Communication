package edu.nitt.spider.usbcommunication;

public interface IUsbConnectionHandler {

    void onUsbStopped();

    void onErrorLooperRunningAlready();

    void onDeviceNotFound();
}