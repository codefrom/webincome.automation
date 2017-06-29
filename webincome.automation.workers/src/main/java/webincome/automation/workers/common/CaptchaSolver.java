package webincome.automation.workers.common;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import nu.pattern.OpenCV;

public class CaptchaSolver {
	
	static {
		OpenCV.loadLocally();
	}

	public static String solve(File image) {
		ITesseract tesseract = new Tesseract();
		tesseract.setLanguage("seofast");
		try {
			return tesseract.doOCR(image).trim();
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public static String solve(BufferedImage image) {
		ITesseract tesseract = new Tesseract();
		tesseract.setLanguage("seofast");
		try {
			return tesseract.doOCR(image).trim();			
		} catch (Exception e) {
			return e.getMessage();
		}
	}
		
	public static String solve(Mat mat) {
	    BufferedImage image = new BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_BYTE_GRAY);
	    byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	    mat.get(0, 0, data);
	    return solve(image);
    }

	public static String solveSeoFastDigit(BufferedImage image) {
		return solveSeoFastDigit(img2Mat(image));
	}

	public static String solveSeoFastDigit(String path) {
		Mat im = Imgcodecs.imread(path, 0);
		return solveSeoFastDigit(im);
	}
	
	public static String solveSeoFastDigit(Mat im) {

		Mat bww = new Mat(im.size(), CvType.CV_8UC1);
		Imgproc.bilateralFilter(im, bww, 9, 75, 75);
		
		Mat bw = new Mat(bww.size(), CvType.CV_8U);
		Imgproc.threshold(im, bw, 0, 255, Imgproc.THRESH_OTSU);
		Imgproc.resize(bw, bw, new Size(bw.width() * 10, bw.height() * 10));		
		
		//Imgproc.erode(bw, bw, new Mat(), new Point(0, 0), 10);
		//Imgproc.blur(bw, bw, new Size(30, 30));
		//Imgproc.morphologyEx(bw, bw, Imgproc.MORPH_OPEN, new Mat());

		final double HTHRESH = bw.rows() * 0.1; // bounding-box height threshold
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(bw, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
		final int aproxlevel = 3;
		bw.setTo(new Scalar(0));
		for (int i = 0; i < contours.size(); i++)
		{
			MatOfPoint contour = contours.get(i);

			Rect rect = Imgproc.boundingRect(contour);
		    if (rect.height < HTHRESH)
		    	continue;
	        if (rect.width >= bw.width())
            	continue;

            MatOfPoint2f approx = new MatOfPoint2f();
	    	Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approx, aproxlevel, true);
	    	MatOfPoint approxx = new MatOfPoint(approx.toArray());
	    	Point zero = approx.toArray()[0];
	    	double[] zrocolor = bw.get((int)zero.y, (int)zero.x);
	    	Imgproc.drawContours(bw, Arrays.asList(approxx), 0, new Scalar(255-zrocolor[0]), Core.FILLED);
		}
		
		Imgproc.threshold(bw, bw, 0, 255, Imgproc.THRESH_BINARY_INV);
	    String res = solve(bw).replaceAll("[^1-9]", "");
	    if (res.length() != 1) {
	    	long time = System.currentTimeMillis();
	    	Imgcodecs.imwrite("C:\\tmp\\digit_orig_" + time + ".png", im);
	    	Imgcodecs.imwrite("C:\\tmp\\digit_proc_" + time + ".png", bw);
	    	res = "8";
	    }
	    
	    return res;
	}
	
	private static Mat img2Mat(BufferedImage in) {
        Mat out;
        byte[] data;
        int r, g, b;

        if (in.getType() == BufferedImage.TYPE_INT_RGB) {
            out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC3);
            data = new byte[in.getWidth() * in.getHeight() * (int) out.elemSize()];
            int[] dataBuff = in.getRGB(0, 0, in.getWidth(), in.getHeight(), null, 0, in.getWidth());
            for (int i = 0; i < dataBuff.length; i++) {
                data[i * 3] = (byte) ((dataBuff[i] >> 0) & 0xFF);
                data[i * 3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
                data[i * 3 + 2] = (byte) ((dataBuff[i] >> 16) & 0xFF);
            }
        } else {
            out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC1);
            data = new byte[in.getWidth() * in.getHeight() * (int) out.elemSize()];
            int[] dataBuff = in.getRGB(0, 0, in.getWidth(), in.getHeight(), null, 0, in.getWidth());
            for (int i = 0; i < dataBuff.length; i++) {
                r = (byte) ((dataBuff[i] >> 0) & 0xFF);
                g = (byte) ((dataBuff[i] >> 8) & 0xFF);
                b = (byte) ((dataBuff[i] >> 16) & 0xFF);
                data[i] = (byte) ((0.21 * r) + (0.71 * g) + (0.07 * b));
            }
        }
        out.put(0, 0, data);
        return out;
    }
	
	public static String solveSeoFastMath(BufferedImage image) {
		return solveSeoFastMath(img2Mat(image));	
	}
	
	public static String solveSeoFastMath(String path) {
		Mat im = Imgcodecs.imread(path, 0);
		return solveSeoFastMath(im);	
	}
	
	public static String solveSeoFastMath(Mat im) {

		Mat bww = new Mat(im.size(), CvType.CV_8UC1);
		Imgproc.bilateralFilter(im, bww, 9, 75, 75);

		Mat bw = new Mat(bww.size(), CvType.CV_8UC1);
		Imgproc.threshold(bww, bw, 0, 255, Imgproc.THRESH_OTSU);
		Imgproc.resize(bw, bw, new Size(bw.width() * 10, bw.height() * 10));
	
		Imgproc.erode(bw, bw, new Mat(), new Point(0, 0), 20);
		Imgproc.blur(bw, bw, new Size(30, 30));
		Imgproc.morphologyEx(bw, bw, Imgproc.MORPH_OPEN, new Mat());
		Imgproc.threshold(bw, bw, 0, 255, Imgproc.THRESH_BINARY_INV);
		
		final double HTHRESH = bw.rows() * 0.5; // bounding-box height threshold
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(bw, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
		final int aproxlevel = 3;
		bw.setTo(new Scalar(0));
		for (int i = 0; i < contours.size(); i++)
		{
			MatOfPoint contour = contours.get(i);

			Rect rect = Imgproc.boundingRect(contour);
		    if (rect.height < HTHRESH)
		    	continue;
	        if (rect.width >= bw.width())
            	continue;

            if (rect.width > HTHRESH) {
	            Point[] points = contour.toArray();
	            ArrayList<Point> leftPoints = new ArrayList<Point>();
	            ArrayList<Point> rightPoints = new ArrayList<Point>();
	            for (int j = 0; j < points.length; j++) {
					if (points[j].x > (rect.x + rect.width/2) + 5 )
						rightPoints.add(points[j]);
					else if (points[j].x < (rect.x + rect.width/2) + 5 )
						leftPoints.add(points[j]);
				}	            
	            
	            MatOfPoint2f approxLeft = new MatOfPoint2f();
		    	Imgproc.approxPolyDP(new MatOfPoint2f(leftPoints.toArray(new Point[0])), approxLeft, aproxlevel, true);
		    	MatOfPoint approxxLeft = new MatOfPoint(approxLeft.toArray());
		    	Imgproc.drawContours(bw, Arrays.asList(approxxLeft), 0, new Scalar(255), Core.FILLED);
		    	
	            MatOfPoint2f approxRight = new MatOfPoint2f();
		    	Imgproc.approxPolyDP(new MatOfPoint2f(rightPoints.toArray(new Point[0])), approxRight, aproxlevel, true);
		    	MatOfPoint approxxRight = new MatOfPoint(approxRight.toArray());
		    	Imgproc.drawContours(bw, Arrays.asList(approxxRight), 0, new Scalar(255), Core.FILLED);
            } else {
	            MatOfPoint2f approx = new MatOfPoint2f();
		    	Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approx, aproxlevel, true);
		    	MatOfPoint approxx = new MatOfPoint(approx.toArray());
		    	Imgproc.drawContours(bw, Arrays.asList(approxx), 0, new Scalar(255), Core.FILLED);
		    }   
		}
		Imgproc.threshold(bw, bw, 0, 255, Imgproc.THRESH_BINARY_INV);

	    String res = solve(bw).replace(" ", "+").replace("++","+").replaceAll("[^1-9+-=]", "");
	    
	    if (!res.matches("\\d.\\d")) {
	    	long time = System.currentTimeMillis();
	    	Imgcodecs.imwrite("c:\\tmp\\math_orig_" + time + ".png", im);
	    	Imgcodecs.imwrite("c:\\tmp\\YADisk\\Work\\Pets\\WebIncomeAutomator\\Seo-Fast\\CaptchaExamplex\\errors\\math_proc_" + time + ".png", bw);
	    	//res="1+1";
	    }
	    return res; 
	}

}
