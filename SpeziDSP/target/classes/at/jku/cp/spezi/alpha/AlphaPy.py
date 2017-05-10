import numpy as np
import string
import madmom




def get_frames_from_spec(spec,input_size = 105,context_size = 7,frame_size=15): 
    
    #limit the spectrum and apply zero-padding
    spec = spec[:,0:input_size] 
    spec=np.vstack([np.zeros((context_size, input_size),dtype=np.float32), spec, np.zeros((context_size, input_size),dtype=np.float32)])    
    frames = madmom.utils.segment_axis(spec, frame_size,1,axis=0)
  
    return frames

#filterbands = 24,fmin = 65,fmax=2100
def get_spectogram(path_to_music, frame_size=8192, hop_size=4410,filterbands = 12,fmin = 30, fmax=17000):  

    spec = madmom.audio.spectrogram.LogarithmicFilteredSpectrogramProcessor(num_bands=filterbands,fmin=fmin,fmax=fmax) \
            .process(path_to_music, frame_size=frame_size,hop_size=hop_size, num_channels = 1, dtype=np.float32)
    return spec

path_to_music = "train/train1.wav"


spec = get_spectogram(path_to_music)

print "test"