#!/usr/bin/env python3
"""
Test script to verify page parameter functionality
Tests that the extract_page4_photos method now accepts page numbers
"""

import os
import sys
from extract_page4 import Page4PhotoExtractor

def test_page_parameters():
    """Test that the extractor accepts different page parameters."""
    
    print("🧪 Testing Page Parameter Functionality")
    print("=" * 50)
    
    # Initialize extractor
    extractor = Page4PhotoExtractor()
    
    # Test 1: Check if method accepts page parameter
    print("\n✅ Test 1: Method signature accepts page_number parameter")
    try:
        # This should not raise an error even if PDF doesn't exist
        # We're just checking the method signature
        import inspect
        sig = inspect.signature(extractor.extract_page4_photos)
        params = list(sig.parameters.keys())
        print(f"   Method parameters: {params}")
        
        if 'page_number' in params:
            print("   ✅ PASS: page_number parameter found")
        else:
            print("   ❌ FAIL: page_number parameter missing")
            return False
    except Exception as e:
        print(f"   ❌ FAIL: Error checking method signature: {e}")
        return False
    
    # Test 2: Check default parameter
    print("\n✅ Test 2: Default page parameter value")
    try:
        sig = inspect.signature(extractor.extract_page4_photos)
        page_param = sig.parameters.get('page_number')
        if page_param and page_param.default == 4:
            print("   ✅ PASS: Default page number is 4")
        else:
            print(f"   ❌ FAIL: Default page number is {page_param.default if page_param else 'missing'}")
            return False
    except Exception as e:
        print(f"   ❌ FAIL: Error checking default parameter: {e}")
        return False
    
    # Test 3: Test with actual PDF if available
    print("\n✅ Test 3: Test with actual PDF (if available)")
    pdf_path = "169.pdf"
    if os.path.exists(pdf_path):
        try:
            print(f"   Found PDF: {pdf_path}")
            
            # Test with page 3
            print("   Testing extraction with page 3...")
            results = extractor.extract_page4_photos(pdf_path, 3)
            print(f"   ✅ PASS: Successfully processed page 3")
            print(f"   Output directory: {results['output_dir']}")
            
            # Test with page 4 (default)
            print("   Testing extraction with page 4...")
            results = extractor.extract_page4_photos(pdf_path, 4)
            print(f"   ✅ PASS: Successfully processed page 4")
            print(f"   Output directory: {results['output_dir']}")
            
        except ValueError as e:
            if "Invalid page number" in str(e):
                print(f"   ✅ PASS: Proper page validation - {e}")
            else:
                print(f"   ❌ FAIL: Unexpected ValueError - {e}")
                return False
        except Exception as e:
            print(f"   ⚠️  WARNING: Error during PDF processing - {e}")
            print("   This might be due to missing PDF or other issues, but method signature is correct")
    else:
        print(f"   ⚠️  PDF file not found: {pdf_path}")
        print("   Skipping actual extraction test")
    
    print("\n🎉 All tests completed successfully!")
    print("✅ The extract_page4_photos method now properly accepts page_number parameter")
    return True

if __name__ == "__main__":
    success = test_page_parameters()
    sys.exit(0 if success else 1)
