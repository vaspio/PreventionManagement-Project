package com.project.test1;

public class TimeStep {
    private String time_step;
    private String longit;
    private String lat;
    public TimeStep(String ts, String lat, String longit){
        this.time_step = ts;
        this.lat = lat;
        this.longit = longit;
    }
    public void setLat(String lat){this.lat = lat;}

    public String getLat() { return lat; }

    public void setLongit(String longt){this.longit = longt;}

    public String getLongit() { return longit;}

    public void setTime_step(String ts){this.time_step = ts;}

    public String getTime_step() { return time_step;}
}
