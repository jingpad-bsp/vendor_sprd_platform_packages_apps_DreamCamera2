/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

#ifndef _LIBSRLITE_H
#define _LIBSRLITE_H
#include <string>

int SrLiteInit(void **handle,const std::string model_name,const int number_of_threads, const bool accel, 
               const bool gl_backend, const bool allow_fp16);

int SrLiteRun(void *handle, const uint8_t* image, const int image_width,const int image_height,
                const int image_channels, uint8_t* result, int stride, int scale);
                
int SrLiteDeInit(void *handle);

#endif  // _LIBSRLITE_H
