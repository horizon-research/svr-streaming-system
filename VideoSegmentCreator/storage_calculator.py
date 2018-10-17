import os

if __name__ == '__main__':
    # dest_lst = ['rhino-fov', 'elephant-fov', 'paris-fov', 'nyc-fov', 'roller-fov']
    dest_lst = ['rhino-fov']
    percentages = [0.25, 0.50, 0.75, 1.0]
    size_in_bytes = 0

    for dest in dest_lst:
        print(dest)
        for percentage in percentages:
            for subdir in os.listdir(dest):
                subdir = os.path.join(dest, subdir)
                total_file_num = len(os.listdir(subdir))
                file_num_we_need = int(round(total_file_num * percentage))
                # print(file_num_we_need, total_file_num)
                for filename in os.listdir(subdir):
                    if file_num_we_need > 0:
                        filename = os.path.join(subdir, filename)
                        file_num_we_need = file_num_we_need - 1
                        st = os.stat(filename)
                        print(st.st_size / 1024)
                        size_in_bytes = size_in_bytes + st.st_size
                    else:
                        break
            # print(percentage, size_in_bytes)
            size_in_bytes = 0
