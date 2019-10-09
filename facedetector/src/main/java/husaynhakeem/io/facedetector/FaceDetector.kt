package husaynhakeem.io.facedetector

import android.graphics.Bitmap
import android.widget.Toast
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import husaynhakeem.io.facedetector.models.Frame

class FaceDetector(private val faceBoundsOverlay: FaceBoundsOverlay) {

    private val faceBoundsOverlayHandler = FaceBoundsOverlayHandler()
    private val firebaseFaceDetectorWrapper = FirebaseFaceDetectorWrapper()

    fun process(frame: Frame,
                onNoFaceDetected: () -> Unit,
                onExactOneFaceDetected: (FirebaseVisionFace) -> Unit,
                onMoreFaceDetected: () -> Unit) {
        updateOverlayAttributes(frame)
        detectFacesIn(frame)

        faceBoundsOverlay.onNoFaceDetected = onNoFaceDetected
        faceBoundsOverlay.onExactOneFaceDetected = onExactOneFaceDetected
        faceBoundsOverlay.onMoreFaceDetected = onMoreFaceDetected
    }

    fun closeDetector() {
        firebaseFaceDetectorWrapper.closeDetector()
    }

    fun getImageBitmap(): Bitmap? {
        return faceBoundsOverlay.getImageBitmap()
    }

    private fun updateOverlayAttributes(frame: Frame) {
        faceBoundsOverlayHandler.updateOverlayAttributes(
            overlayWidth = frame.size.width,
            overlayHeight = frame.size.height,
            rotation = frame.rotation,
            isCameraFacingBack = frame.isCameraFacingBack,
            callback = { newWidth, newHeight, newOrientation, newFacing ->
                faceBoundsOverlay.cameraPreviewWidth = newWidth
                faceBoundsOverlay.cameraPreviewHeight = newHeight
                faceBoundsOverlay.cameraOrientation = newOrientation
                faceBoundsOverlay.cameraFacing = newFacing
            })
    }

    private fun detectFacesIn(frame: Frame) {
        frame.data?.let {
            val image = convertFrameToImage(frame)
            firebaseFaceDetectorWrapper.process(
                image = image,
                onSuccess = { faces ->
                    faceBoundsOverlay.updateFaces(faces, image)
                },
                onError = {
                    Toast.makeText(faceBoundsOverlay.context, "Error processing images: $it", Toast.LENGTH_LONG).show()
                })
        }
    }

    private fun convertFrameToImage(frame: Frame) =
        FirebaseVisionImage.fromByteArray(frame.data!!, extractFrameMetadata(frame))

    private fun extractFrameMetadata(frame: Frame): FirebaseVisionImageMetadata =
        FirebaseVisionImageMetadata.Builder()
            .setWidth(frame.size.width)
            .setHeight(frame.size.height)
            .setFormat(frame.format)
            .setRotation(frame.rotation / RIGHT_ANGLE)
            .build()

    companion object {
        private const val RIGHT_ANGLE = 90
    }
}