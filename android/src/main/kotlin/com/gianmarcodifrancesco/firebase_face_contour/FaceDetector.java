package com.gianmarcodifrancesco.firebase_face_contour;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import io.flutter.plugin.common.MethodChannel;

class FaceDetector implements Detector {
    static final FaceDetector instance = new FaceDetector();

    private FaceDetector() {
    }

    private FirebaseVisionFaceDetector detector;
    private Map<String, Object> lastOptions;

    @Override
    public void handleDetection(
            FirebaseVisionImage image, Map<String, Object> options, final MethodChannel.Result result) {

        // Use instantiated detector if the options are the same. Otherwise, close and instantiate new
        // options.

        if (detector == null) {
            lastOptions = options;
            detector = FirebaseVision.getInstance().getVisionFaceDetector(parseOptions(lastOptions));
        } else if (!options.equals(lastOptions)) {
            try {
                detector.close();
            } catch (IOException e) {
                result.error("faceDetectorIOError", e.getLocalizedMessage(), null);
                return;
            }

            lastOptions = options;
            detector = FirebaseVision.getInstance().getVisionFaceDetector(parseOptions(lastOptions));
        }

        detector
                .detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionFace>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                                List<Map<String, Object>> faces = new ArrayList<>(firebaseVisionFaces.size());
                                for (FirebaseVisionFace face : firebaseVisionFaces) {
                                    Map<String, Object> faceData = new HashMap<>();

                                    faceData.put("left", (double) face.getBoundingBox().left);
                                    faceData.put("top", (double) face.getBoundingBox().top);
                                    faceData.put("width", (double) face.getBoundingBox().width());
                                    faceData.put("height", (double) face.getBoundingBox().height());

                                    faceData.put("headEulerAngleY", face.getHeadEulerAngleY());
                                    faceData.put("headEulerAngleZ", face.getHeadEulerAngleZ());

                                    if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                        faceData.put("smilingProbability", face.getSmilingProbability());
                                    }

                                    if (face.getLeftEyeOpenProbability()
                                            != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                        faceData.put("leftEyeOpenProbability", face.getLeftEyeOpenProbability());
                                    }

                                    if (face.getRightEyeOpenProbability()
                                            != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                        faceData.put("rightEyeOpenProbability", face.getRightEyeOpenProbability());
                                    }

                                    if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                                        faceData.put("trackingId", face.getTrackingId());
                                    }

                                    faceData.put("landmarks", getLandmarkData(face));
                                    faceData.put("contours", getContourData(face));

                                    faces.add(faceData);
                                }

                                result.success(faces);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                result.error("faceDetectorError", exception.getLocalizedMessage(), null);
                            }
                        });
    }

    private Map<String, double[]> getLandmarkData(FirebaseVisionFace face) {
        Map<String, double[]> landmarks = new HashMap<>();

        landmarks.put("bottomMouth", landmarkPosition(face, FirebaseVisionFaceLandmark.MOUTH_BOTTOM));
        landmarks.put("leftCheek", landmarkPosition(face, FirebaseVisionFaceLandmark.LEFT_CHEEK));
        landmarks.put("leftEar", landmarkPosition(face, FirebaseVisionFaceLandmark.LEFT_EAR));
        landmarks.put("leftEye", landmarkPosition(face, FirebaseVisionFaceLandmark.LEFT_EYE));
        landmarks.put("leftMouth", landmarkPosition(face, FirebaseVisionFaceLandmark.MOUTH_LEFT));
        landmarks.put("noseBase", landmarkPosition(face, FirebaseVisionFaceLandmark.NOSE_BASE));
        landmarks.put("rightCheek", landmarkPosition(face, FirebaseVisionFaceLandmark.RIGHT_CHEEK));
        landmarks.put("rightEar", landmarkPosition(face, FirebaseVisionFaceLandmark.RIGHT_EAR));
        landmarks.put("rightEye", landmarkPosition(face, FirebaseVisionFaceLandmark.RIGHT_EYE));
        landmarks.put("rightMouth", landmarkPosition(face, FirebaseVisionFaceLandmark.MOUTH_RIGHT));

        return landmarks;
    }

    private Map<String, ArrayList<double[]>> getContourData(FirebaseVisionFace face) {
        Map<String, ArrayList<double[]>> contours = new HashMap<>();

        contours.put("allPoints", contoursPoints(face, FirebaseVisionFaceContour.ALL_POINTS));
        contours.put("face", contoursPoints(face, FirebaseVisionFaceContour.FACE));
        contours.put("leftEyebrowTop", contoursPoints(face, FirebaseVisionFaceContour.LEFT_EYEBROW_TOP));
        contours.put("leftEyebrowBottom", contoursPoints(face, FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM));
        contours.put("rightEyebrowTop", contoursPoints(face, FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP));
        contours.put("rightEyebrowBottom", contoursPoints(face, FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM));
        contours.put("leftEye", contoursPoints(face, FirebaseVisionFaceContour.LEFT_EYE));
        contours.put("rightEye", contoursPoints(face, FirebaseVisionFaceContour.RIGHT_EYE));
        contours.put("upperLipTop", contoursPoints(face, FirebaseVisionFaceContour.UPPER_LIP_TOP));
        contours.put("upperLipBottom", contoursPoints(face, FirebaseVisionFaceContour.UPPER_LIP_BOTTOM));
        contours.put("lowerLipTop", contoursPoints(face, FirebaseVisionFaceContour.LOWER_LIP_TOP));
        contours.put("lowerLipBottom", contoursPoints(face, FirebaseVisionFaceContour.LOWER_LIP_BOTTOM));
        contours.put("noseBridge", contoursPoints(face, FirebaseVisionFaceContour.NOSE_BRIDGE));
        contours.put("noseBottom", contoursPoints(face, FirebaseVisionFaceContour.NOSE_BOTTOM));

        return contours;

    }

    private ArrayList<double[]> contoursPoints(FirebaseVisionFace face, int contourInt) {
        FirebaseVisionFaceContour contour = face.getContour(contourInt);
        if (contour != null) {
            List<FirebaseVisionPoint> firebaseVisionPoints = contour.getPoints();
            ArrayList<double[]> contoursArrayList = new ArrayList<>();
            for (FirebaseVisionPoint point : firebaseVisionPoints) {
                contoursArrayList.add(new double[]{point.getX(), point.getY()});
            }
            return contoursArrayList;
        }

        return null;
    }

    private double[] landmarkPosition(FirebaseVisionFace face, int landmarkInt) {
        FirebaseVisionFaceLandmark landmark = face.getLandmark(landmarkInt);
        if (landmark != null) {
            return new double[]{landmark.getPosition().getX(), landmark.getPosition().getY()};
        }

        return null;
    }

    private FirebaseVisionFaceDetectorOptions parseOptions(Map<String, Object> options) {
        int classification =
                (boolean) options.get("enableClassification")
                        ? FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS
                        : FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS;

        int landmark =
                (boolean) options.get("enableLandmarks")
                        ? FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS
                        : FirebaseVisionFaceDetectorOptions.NO_LANDMARKS;

        int contour =
                (boolean) options.get("enableContours")
                        ? FirebaseVisionFaceDetectorOptions.ALL_CONTOURS
                        : FirebaseVisionFaceDetectorOptions.NO_CONTOURS;

        int mode;
        switch ((String) options.get("mode")) {
            case "accurate":
                mode = FirebaseVisionFaceDetectorOptions.ACCURATE;
                break;
            case "fast":
                mode = FirebaseVisionFaceDetectorOptions.FAST;
                break;
            default:
                throw new IllegalArgumentException("Not a mode:" + options.get("mode"));
        }

        FirebaseVisionFaceDetectorOptions.Builder firebaseVisionFaceDetectorOptionsBuilder =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(classification)
                        .setLandmarkMode(landmark)
                        .setMinFaceSize((float) ((double) options.get("minFaceSize")))
                        .setPerformanceMode(mode)
                        .setContourMode(contour);

        if ((boolean) options.get("enableTracking")) {
            firebaseVisionFaceDetectorOptionsBuilder.enableTracking();
        }

        return firebaseVisionFaceDetectorOptionsBuilder.build();
    }
}
