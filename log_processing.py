def calculate_average(file_path):
    with open(file_path, 'r') as file:
        total_time_sum = 0
        jdbc_time_sum = 0
        count = 0
        for line in file:
            try:
                total_time, jdbc_time = map(int, line.split())
                total_time_sum += total_time
                jdbc_time_sum += jdbc_time
                count += 1
            except ValueError:
                pass

        if count > 0:
            total_time_average = total_time_sum / count
            jdbc_time_average = jdbc_time_sum / count
            # both of these times are in nanoseconds, i need them in miliseconds
            total_time_average /= 1000000
            jdbc_time_average /= 1000000
            print(f"Average Total Servlet Time (TS): {total_time_average}")
            print(f"Average JDBC Time (TJ): {jdbc_time_average}")
        else:
            print("No valid numbers found in the file.")


if __name__ == "__main__":
    file_path = "logs/timing.txt"
    calculate_average(file_path)
